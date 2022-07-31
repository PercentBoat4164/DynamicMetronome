package dynamicmetronome.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dynamicmetronome.activities.databinding.MainActivityBinding
import dynamicmetronome.metronome.Metronome

lateinit var mainMetronome: Metronome  /**@todo Find a better way to do this.*/

class MainActivity : AppCompatActivity() {
    lateinit var mainActivity: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = DataBindingUtil.setContentView(this, R.layout.main_activity)
        mainMetronome = Metronome(null, applicationContext, R.raw.metronome_sample)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  // @todo Find a way to make the orientation not break things.

        // Set up the main activity
        mainActivity.TempoNumberPicker.minValue = MIN_TEMPO.toInt()
        mainActivity.TempoNumberPicker.maxValue = MAX_TEMPO.toInt()
        mainActivity.TempoNumberPicker.value = STARTING_TEMPO
        mainActivity.TempoNumberPicker.wrapSelectorWheel = false
        mainActivity.TempoNumberPicker.displayedValues = Array(MAX_TEMPO.toInt()){(it + MIN_TEMPO.toInt()).toString()}
        mainActivity.TempoNumberPicker.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int ->
            mainMetronome.tempo = tempo
            mainActivity.TempoSeekbar.progress = (tempo.toFloat() / (MAX_TEMPO - MIN_TEMPO) * 100).toInt()
        }
        mainActivity.QuarterPlaying.isChecked = true
        mainActivity.QuarterPlaying.setOnCheckedChangeListener{
                _: CompoundButton, isChecked: Boolean ->
            if (!isChecked) {
                mainMetronome.volume = 0F
            } else {
                mainMetronome.volume = mainActivity.QuarterVolume.progress / 100F
            }
        }
        mainActivity.StartStopButton.setOnClickListener{
            mainMetronome.togglePlaying()
        }
        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mainActivity.QuarterPlaying.isChecked) {
                    mainMetronome.volume = progress / 100F
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = (100 * STARTING_QUARTER_VOLUME).toInt()
        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mainMetronome.tempo = (progress * (MAX_TEMPO - MIN_TEMPO) / 100F + MIN_TEMPO).toInt()
                    mainActivity.TempoNumberPicker.value = mainMetronome.tempo
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
        const val MIN_TEMPO = 20f
        const val MAX_TEMPO = 500f
        const val STARTING_TEMPO = 130
        const val BEATS_PER_MEASURE = 4.0
        const val STARTING_QUARTER_VOLUME = .33F

        init {
//            System.loadLibrary("soundPlayer")
        }
    }
}