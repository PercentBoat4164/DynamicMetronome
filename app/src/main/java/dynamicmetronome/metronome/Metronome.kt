package dynamicmetronome.metronome

import android.content.res.AssetFileDescriptor
import android.media.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.Semaphore

// @todo Split this into a Metronome class and a Sound class
class Metronome {
    private var program: Program? = null
    var clickCallback: (Float) -> Unit = {}
    private var clickSemaphore = Semaphore(0)
    private var clickCallbackThread = Thread {
        while (true) {
            clickSemaphore.acquire()
            clickCallback(viewPlayHead)
        }
    }
    var stopCallback: () -> Unit = {}
    private lateinit var sound: FloatArray
    private val playingMutex = Mutex()
    private var programPlayHead = 0
    private var viewPlayHead = 0f
    private var soundPlayHead = 0
    private var instructions = doubleArrayOf()

    private var framesToNextClick = 0
    private var frameNumber = 0

    var playing = false
    var volume = 1f
    var tempo = 130.0

    init {
        clickCallbackThread.start()

        // @todo When adding sound options, un-hardcode the 48000Hz below.
        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(48000).build()
        val audioTrack = AudioTrack.Builder().setAudioFormat(audioFormat).setBufferSizeInBytes(AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)).build()
        frameNumber = framesToNextClick + 1
        audioTrack.play()
        runBlocking { playingMutex.lock() }
        val thread = Thread {
            while (true) {
                if (instructions.isNotEmpty()) framesToNextClick = (1 / instructions[0] * 48000 * 120).toInt()
                soundPlayHead = 0
                programPlayHead = 0
                frameNumber = 0
                viewPlayHead = 0f
                while (!playingMutex.isLocked) {
                    val data = FloatArray(DEFAULT_BUFFER_SIZE)
                    if (instructions.isEmpty()) {
                        // What happens when playing normally
                        framesToNextClick = (1 / tempo * 48000 * 120).toInt()
                        for (frame in 0 until DEFAULT_BUFFER_SIZE) {
                            if (frameNumber++ >= framesToNextClick) {
                                // Start the click callback thread
                                clickSemaphore.release()
                                soundPlayHead = 0
                                frameNumber = 0
                            }
                            // @todo When adding sound options, for multi-channel support add a loop over the number of channels here.
                            if (soundPlayHead < sound.size) data[frame] = sound[soundPlayHead++] * volume * 2
                        }
                    } else {
                        // What happens when playing a program
                        for (frame in 0 until DEFAULT_BUFFER_SIZE) {
                            if (frameNumber++ >= framesToNextClick) {
                                if (programPlayHead >= instructions.size) {
                                    stop()
                                    break
                                }
                                // Start the click callback thread
                                clickSemaphore.release()
                                framesToNextClick = (1 / instructions[programPlayHead++] * 48000 * 120).toInt()
                                soundPlayHead = 0
                                frameNumber = 0
                                viewPlayHead += 1/4f
                            }
                            // @todo When adding sound options, for multi-channel support add a loop over the number of channels here.
                            if (soundPlayHead < sound.size) data[frame] = sound[soundPlayHead++] * volume * 2
                        }
                    }
                    audioTrack.write(data, 0, DEFAULT_BUFFER_SIZE, AudioTrack.WRITE_BLOCKING)
                }
                if (playingMutex.isLocked) {
                    runBlocking { playingMutex.lock() }
                    playingMutex.unlock()
                }
            }
        }
        thread.priority = Thread.MAX_PRIORITY
        thread.start()
    }

    fun start() {
        if (!playing) {
            playing = true
            if (playingMutex.isLocked) playingMutex.unlock()
        }
    }

    fun stop() {
        if (playing) {
            playing = false
            stopCallback()
            runBlocking { playingMutex.lock() }
        }
    }

    fun useSound(stream: AssetFileDescriptor) {
        // Get file contents
        val extractor = MediaExtractor()
        extractor.setDataSource(stream.fileDescriptor, stream.startOffset, stream.length)
        extractor.selectTrack(0)

        val decoder = MediaCodec.createByCodecName(
            MediaCodecList(MediaCodecList.REGULAR_CODECS)
                .findDecoderForFormat(extractor.getTrackFormat(0))
        )
        decoder.configure(extractor.getTrackFormat(0), null, null, 0)
        decoder.start()
        val outputBuffers = ArrayList<ByteArray>()
        val info = MediaCodec.BufferInfo()
        var isEOS = false
        while (!isEOS) {
            val inIndex = decoder.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val buffer = decoder.getInputBuffer(inIndex)!!
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(
                        inIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    decoder.queueInputBuffer(
                        inIndex,
                        0,
                        sampleSize,
                        extractor.sampleTime,
                        0)
                    extractor.advance()
                }
            }
            val outIndex = decoder.dequeueOutputBuffer(info, 10000)
            if (outIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
              && outIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                val outBuffer = decoder.getOutputBuffer(outIndex)
                outputBuffers.add(ByteArray(info.size))
                outBuffer?.get(outputBuffers[outputBuffers.size - 1])
                outBuffer?.clear()
                decoder.releaseOutputBuffer(outIndex, false)
            }
        }
        val format = decoder.outputFormat
        decoder.stop()
        decoder.release()
        extractor.release()

        // Merge arraylists
        var size = 0
        for (outputBuffer in outputBuffers) size += outputBuffer.size
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val buffer = ByteArray(size / channels)
        size = 0
        for (outputBuffer in outputBuffers) for (i in 0 until outputBuffer.size / channels) buffer[size++] = outputBuffer[i * channels]
        sound = FloatArray(buffer.size / 2)
        // Convert MediaCodec output to float samples
        for (i in 0 until buffer.size / 2) sound[i] = (buffer[i * 2 + 1].toInt().shl(8) + buffer[i * 2].toInt()).toFloat() / 65535
    }

    fun getProgram(): Program? = program

    fun setProgram(program: Program?) {
        this.program = program
        instructions = program?.getTempos() ?: doubleArrayOf()
    }
}
