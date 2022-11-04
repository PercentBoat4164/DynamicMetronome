package dynamicmetronome.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.AssetFileDescriptor
import android.media.*
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dynamicmetronome.activities.databinding.MainActivityBinding
import dynamicmetronome.metronome.Metronome
import java.nio.ByteBuffer

val mainMetronome: Metronome = Metronome() /**@todo Find a better way to do this.*/

class MainActivity : AppCompatActivity() {
    lateinit var mainActivity: MainActivityBinding

    fun decode(stream: AssetFileDescriptor) {
        val extractor = MediaExtractor()
        extractor.setDataSource(stream.fileDescriptor, stream.startOffset, stream.length)

        var decoder = MediaCodec.createByCodecName(MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(extractor.getTrackFormat(0)))

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime!!.startsWith("audio/")) {
                extractor.selectTrack(i)
                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, 48000, 2), null, null, 0)
                break
            }
        }

        decoder.start();

        val outputBuffers = ArrayList<ByteBuffer>()
        val info = MediaCodec.BufferInfo()
        var isEOS = false

        while (!isEOS) {
            val inIndex = decoder.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val buffer = decoder.getInputBuffer(inIndex)!!
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    // We shouldn't stop the recording at this point, just pass the EOS
                    // flag to decoder, we will get it again from the
                    // dequeueOutputBuffer
                    Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                    decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            when (val outIndex = decoder.dequeueOutputBuffer(info, 10000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d("DecodeActivity", "New format " + decoder.outputFormat)
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    Log.d("DecodeActivity", "dequeueOutputBuffer timed out!")
                }
                else -> {
                    outputBuffers.add(decoder.getOutputBuffer(outIndex)!!)
                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = DataBindingUtil.setContentView(this, R.layout.main_activity)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  // @todo Find a way to make the orientation not break things.

        mainMetronome.useSound(resources.openRawResourceFd(R.raw.metronome_sample))

        // Set up the main activity
        mainActivity.TempoNumberPicker.minValue = MIN_TEMPO.toInt()
        mainActivity.TempoNumberPicker.maxValue = MAX_TEMPO.toInt()
        mainActivity.TempoNumberPicker.value = STARTING_TEMPO
        mainActivity.TempoNumberPicker.wrapSelectorWheel = false
        mainActivity.TempoNumberPicker.displayedValues = Array(MAX_TEMPO.toInt()){(it + MIN_TEMPO.toInt()).toString()}
        mainActivity.TempoNumberPicker.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int ->
            mainMetronome.setTempo(tempo.toDouble())
            val tempoRange = MAX_TEMPO - MIN_TEMPO
            if (tempo.toDouble() == MAX_TEMPO) {
                mainActivity.TempoSeekbar.progress = 100
            } else {
                mainActivity.TempoSeekbar.progress = 1 + ((tempo - MIN_TEMPO) * (100 - 1) / (MAX_TEMPO - MIN_TEMPO)).toInt()
            }
        }
        mainActivity.QuarterPlaying.isChecked = true
        mainActivity.QuarterPlaying.setOnCheckedChangeListener{
                _: CompoundButton, isChecked: Boolean ->
            if (!isChecked) {
                mainMetronome.setVolume(0.0)
            } else {
                mainMetronome.setVolume(mainActivity.QuarterVolume.progress / 100.0)
            }
        }
        mainActivity.StartStopButton.setOnClickListener{
            mainMetronome.togglePlaying()
        }
        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mainActivity.QuarterPlaying.isChecked) {
                    mainMetronome.setVolume(progress / 100.0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (mainActivity.QuarterPlaying.isChecked) {
                    mainMetronome.setVolume(seekBar!!.progress / 100.0)
                }
            }
        })
        mainActivity.QuarterVolume.progress = (100 * STARTING_QUARTER_VOLUME).toInt()
        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val tempo = (progress * (MAX_TEMPO - MIN_TEMPO) / 100F + MIN_TEMPO).toULong()
                    mainMetronome.setTempo(tempo.toInt())
                    mainActivity.TempoNumberPicker.value = tempo.toInt()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.TempoSeekbar.progress = (STARTING_TEMPO.toFloat() / (MAX_TEMPO) * 100).toInt()
        mainActivity.ProgramsButton.setOnClickListener{
            mainMetronome.stop()
            startActivity(Intent(this, ListProgramsActivity::class.java))
        }
    }

    companion object {
        const val MIN_TEMPO = 20.0
        const val MAX_TEMPO = 500.0
        const val STARTING_TEMPO = 130
        const val BEATS_PER_MEASURE = 4.0
        const val STARTING_QUARTER_VOLUME = .33

        init {
            System.loadLibrary("metronome")
        }
    }
}