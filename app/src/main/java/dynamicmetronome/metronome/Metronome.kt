package dynamicmetronome.metronome

import android.content.res.AssetFileDescriptor
import android.media.*
import java.io.Closeable


/**
 * Metronome wrapper class
 *
 * Contains a handle to the C++ object data that is used by this class on the C++ side.
 */

class Metronome : Closeable, AudioDeviceCallback() {
    private var program: Program? = null
    private var onClickCallback: () -> Unit = {}
    private var onStopCallback: () -> Unit = {}

    private val handle: Long = create()
    var playing = false

    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
        super.onAudioDevicesAdded(addedDevices)
        resetPlayer(handle)
    }

    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
        super.onAudioDevicesRemoved(removedDevices)
        resetPlayer(handle)
    }

    // Destructor
    override fun close() = destroy(handle)

    fun start() {
        playing = true
        start(handle)
    }

    fun stop() {
        playing = false
        stop(handle)
    }

    fun useSound(stream: AssetFileDescriptor) {
        // Run this in a separate thread to avoid issues with high application loading times.
        Thread {
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
            for (outputBuffer in outputBuffers) for (i in 0 until outputBuffer.size / channels)
                buffer[size++] = outputBuffer[i * channels]
            val floats = FloatArray(buffer.size / 2)
            // Convert MediaCodec output to float samples
            for (i in 0 until buffer.size / 2) floats[i] =
                (buffer[i * 2 + 1].toInt().shl(8) + buffer[i * 2].toInt()).toFloat() / 65535
            useSound(handle, floats)
        }.start()
    }

    fun getProgram(): Program? = program

    fun setProgram(program: Program?) {
        this.program = program
        setProgram(handle, program?.getTempos() ?: doubleArrayOf())
    }

    fun setTempo(tempo: Double) = setTempo(handle, tempo)
    fun setVolume(volume: Float) = setVolume(handle, volume)


    fun setOnClickCallback(function: () -> Unit) {
        onClickCallback = function
    }
    fun setOnStopCallback(function: () -> Unit) {
        onStopCallback = function
    }

    private fun clickCallback() = onClickCallback()
    private fun stopCallback() {
        stop()
        onStopCallback()
    }

    private external fun create(): Long
    private external fun resetPlayer(handle: Long)
    private external fun destroy(handle: Long)
    private external fun start(handle: Long)
    private external fun stop(handle: Long)
    private external fun useSound(handle: Long, bytes: FloatArray)
    private external fun setProgram(handle: Long, instructions: DoubleArray)
    private external fun clearProgram(handle: Long)
    private external fun setTempo(handle: Long, tempo: Double)
    private external fun setVolume(handle: Long, volume: Float)
}
