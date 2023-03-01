package dynamicmetronome.activities

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.Window
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dynamicmetronome.activities.databinding.MainActivityBinding
import dynamicmetronome.metronome.Metronome

/**@todo Find a better way to do this.*/
val mainMetronome: Metronome = Metronome()

class MainActivity : AppCompatActivity() {
    lateinit var mainActivity: MainActivityBinding
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mainActivity = DataBindingUtil.setContentView(this, R.layout.main_activity)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(mainMetronome, null)

        mainMetronome.useSound(resources.openRawResourceFd(R.raw.metronome_sample))
        mainMetronome.setOnStopCallback { mainActivity.StartStopButton.setImageResource(android.R.drawable.ic_media_play); }

        // Set up the main activity
        mainActivity.TempoNumberPicker.minValue = MIN_TEMPO.toInt()
        mainActivity.TempoNumberPicker.maxValue = MAX_TEMPO.toInt()
        mainActivity.TempoNumberPicker.value = STARTING_TEMPO
        mainActivity.TempoNumberPicker.wrapSelectorWheel = false
        mainActivity.TempoNumberPicker.displayedValues =
            Array(MAX_TEMPO.toInt()) { (it + MIN_TEMPO.toInt()).toString() }
        mainActivity.TempoNumberPicker.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int ->
            mainMetronome.setTempo(tempo.toDouble())
            if (tempo.toDouble() == MAX_TEMPO)
                mainActivity.TempoSeekbar.progress = 100
            else
                mainActivity.TempoSeekbar.progress = 1 + ((tempo - MIN_TEMPO) * (100 - 1) / (MAX_TEMPO - MIN_TEMPO)).toInt()
        }

        mainActivity.QuarterPlaying.isChecked = true
        mainActivity.QuarterPlaying.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (!isChecked)
                mainMetronome.setVolume(0.0F)
            else
                mainMetronome.setVolume(mainActivity.QuarterVolume.progress / 100.0F)
        }

        mainActivity.StartStopButton.setOnClickListener {
            mainMetronome.setProgram(null)
            if (mainMetronome.playing)
                mainMetronome.stop()
            else {
                mainActivity.StartStopButton.setImageResource(android.R.drawable.ic_media_pause)
                mainMetronome.start()
            }
        }

        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mainActivity.QuarterPlaying.isChecked) {
                    if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) mainMetronome.setVolume(progress / 100.0F)
                    else mainMetronome.setVolume(0.0F)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = (100 * STARTING_QUARTER_VOLUME).toInt()

        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val tempo = progress * (MAX_TEMPO - MIN_TEMPO) / 100F + MIN_TEMPO
                    mainMetronome.setTempo(tempo)
                    mainActivity.TempoNumberPicker.value = tempo.toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.TempoSeekbar.progress = (STARTING_TEMPO.toFloat() / (MAX_TEMPO) * 100).toInt()

        mainActivity.ProgramsButton.setOnClickListener {
            mainMetronome.stop()
            startActivity(Intent(this, ListProgramsActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.unregisterAudioDeviceCallback(mainMetronome)
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