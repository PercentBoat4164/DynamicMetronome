package thadd.schelp.dynamicmetronome

import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import thadd.schelp.dynamicmetronome.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val MIN_TEMPO = 20
const val MAX_TEMPO = 300F
const val STARTING_TEMPO = 200

var quarterPlayer = MediaPlayer()

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private var metronomeState = MetronomeState()
    private var metronome = Metronome(metronomeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        quarterPlayer = MediaPlayer.create(this, R.raw.beep)
        quarterPlayer.setVolume(1F, 1F)

        buildGUI()
    }

    private fun buildGUI() {
        mainActivity.Tempo.minValue = MIN_TEMPO
        mainActivity.Tempo.maxValue = MAX_TEMPO.toInt()
        mainActivity.Tempo.value = STARTING_TEMPO
        mainActivity.Tempo.wrapSelectorWheel = false
        mainActivity.Tempo.displayedValues = Array<String>(MAX_TEMPO.toInt()){(it + MIN_TEMPO).toString()}
        mainActivity.Tempo.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int -> metronomeState.tempo = tempo; mainActivity.TempoSeekbar.progress = (tempo.toFloat() / MAX_TEMPO * 100).toInt() }

        mainActivity.QuarterMute.setOnCheckedChangeListener{ _: CompoundButton, isChecked: Boolean -> if (isChecked) { quarterPlayer.setVolume(0F, 0F)} else { quarterPlayer.setVolume(mainActivity.QuarterVolume.progress.toFloat() / 100, mainActivity.QuarterVolume.progress.toFloat() / 100) } }

        mainActivity.StartStopButton.setOnClickListener{ metronome.toggle() }

        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                quarterPlayer.setVolume(progress.toFloat() / 100, progress.toFloat() / 100.toFloat())
                mainActivity.QuarterMute.isChecked = false
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = 10

        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    metronomeState.tempo = (progress * ((MAX_TEMPO - MIN_TEMPO) / 100)).toInt() + MIN_TEMPO
                    mainActivity.Tempo.value = metronomeState.tempo
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.TempoSeekbar.progress = (STARTING_TEMPO.toFloat() / MAX_TEMPO * 100).toInt()
    }
}

class MetronomeState {
    var tempo = STARTING_TEMPO
}

class Metronome(metronomeState: MetronomeState) {
    var state = metronomeState
    var paused = true
    var lock = Object()
//    var scheduler = Executors.newScheduledThreadPool(1)
    var quarterThread = Thread {
        while (!Thread.interrupted()) {
            while (paused) { synchronized(lock) { lock.wait() } }
            quarterPlayer.seekTo(0)
            quarterPlayer.start()
            Thread.sleep((60000 / state.tempo).toLong())
        }
    }
//    var quarterThread = Runnable( object: Runnable, () -> Unit {
//        override fun run() {
//            if (paused) {
//                Log.d("", "Locked!")
//                synchronized(lock) {lock.wait() } }
//            quarterPlayer.seekTo(0)
//            quarterPlayer.start()
//            scheduler.schedule(this, (60000 / state.tempo).toLong(), TimeUnit.MILLISECONDS)
//        }
//    override fun invoke() {}
//})

    init {
        quarterThread.start()
//        scheduler.schedule(quarterThread, 0, TimeUnit.NANOSECONDS)
    }

    fun toggle() {
        paused = !paused
        synchronized(lock) { lock.notifyAll() }
    }
}
