package dynamicmetronome.activities

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dynamicmetronome.activities.databinding.MainActivityBinding
import dynamicmetronome.metronome.Metronome

/**@todo Find a better way to do this. -> One metronome per activity.*/
val mainMetronome: Metronome = Metronome()

class MainActivity : AppCompatActivity() {
    lateinit var mainActivity: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        mainActivity = DataBindingUtil.setContentView(this, R.layout.main_activity)

        mainMetronome.useSound(resources.openRawResourceFd(R.raw.metronome_sample))
        mainMetronome.stopCallback = { mainActivity.StartStopButton.setImageResource(android.R.drawable.ic_media_play); }

        // Set up the main activity
        mainActivity.TempoNumberPicker.minValue = MIN_TEMPO.toInt()
        mainActivity.TempoNumberPicker.maxValue = MAX_TEMPO.toInt()
        mainActivity.TempoNumberPicker.value = mainMetronome.tempo.toInt()
        mainActivity.TempoNumberPicker.wrapSelectorWheel = false
        mainActivity.TempoNumberPicker.displayedValues =
            Array(MAX_TEMPO.toInt()) { (it + MIN_TEMPO.toInt()).toString() }
        mainActivity.TempoNumberPicker.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int ->
            mainMetronome.tempo = tempo.toDouble()
            if (tempo.toDouble() == MAX_TEMPO)
                mainActivity.TempoSeekbar.progress = 100
            else
                mainActivity.TempoSeekbar.progress = 1 + ((tempo - MIN_TEMPO) * (100 - 1) / (MAX_TEMPO - MIN_TEMPO)).toInt()
        }

        mainActivity.QuarterPlaying.isChecked = true
        mainActivity.QuarterPlaying.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (!isChecked)
                mainMetronome.volume = 0.0F
            else
                mainMetronome.volume = (mainActivity.QuarterVolume.progress / 100.0F)
        }

        mainActivity.StartStopButton.setOnClickListener {
            if (mainMetronome.playing) {
                mainMetronome.stop()
            } else {
                mainActivity.StartStopButton.setImageResource(android.R.drawable.ic_media_pause)
                mainMetronome.start()
            }
        }

        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mainActivity.QuarterPlaying.isChecked) mainMetronome.volume = progress / 100.0F
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
                    mainMetronome.tempo = tempo
                    mainActivity.TempoNumberPicker.value = tempo.toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.TempoSeekbar.progress = (mainMetronome.tempo.toFloat() / (MAX_TEMPO) * 100).toInt()

        mainActivity.ProgramsButton.setOnClickListener {
            mainMetronome.stop()
            startActivity(Intent(this, ListProgramsActivity::class.java))
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        mainMetronome.stopCallback = { mainActivity.StartStopButton.setImageResource(android.R.drawable.ic_media_play); }
    }

    companion object {
        const val MIN_TEMPO = 20.0
        const val MAX_TEMPO = 500.0
        const val STARTING_QUARTER_VOLUME = 1 / 3.0
    }
}