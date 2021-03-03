package thadd.schelp.dynamicmetronome

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import thadd.schelp.dynamicmetronome.databinding.ActivityMainBinding

const val MIN_TEMPO = 20
const val MAX_TEMPO = 300F
const val STARTING_TEMPO = 200

@SuppressLint("StaticFieldLeak")
lateinit var appContext: Context

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private var metronomeState = MetronomeState()
    private var metronome = Metronome(metronomeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)
        appContext = applicationContext
        metronome.generateSoundIDs()

        buildGUI()
    }

    private fun buildGUI() {
        mainActivity.Tempo.minValue = MIN_TEMPO
        mainActivity.Tempo.maxValue = MAX_TEMPO.toInt()
        mainActivity.Tempo.value = STARTING_TEMPO
        mainActivity.Tempo.wrapSelectorWheel = false
        mainActivity.Tempo.displayedValues = Array(MAX_TEMPO.toInt()){(it + MIN_TEMPO).toString()}
        mainActivity.Tempo.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int -> metronomeState.setTempo(tempo); mainActivity.TempoSeekbar.progress = (tempo.toFloat() / MAX_TEMPO * 100).toInt() }

        mainActivity.QuarterMute.setOnCheckedChangeListener{ _: CompoundButton, isChecked: Boolean -> if (isChecked) { metronome.setQuarterVolume(0F)} else { metronome.setQuarterVolume(mainActivity.QuarterVolume.progress / 100F) } }

        mainActivity.StartStopButton.setOnClickListener{ metronome.toggle() }

        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                metronome.setQuarterVolume(progress / 100F)
                mainActivity.QuarterMute.isChecked = false
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = 10

        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    metronomeState.setTempo((progress * ((MAX_TEMPO - MIN_TEMPO) / 100)).toInt() + MIN_TEMPO)
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
    var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build()).build()
    var volume = 1F
    var interval = (60000F / tempo).toLong()

    @JvmName("setTempo1")
    fun setTempo(newTempo: Int) {
        tempo = newTempo
        interval = (60000F / tempo).toLong()
    }

    @JvmName("setVolume1")
    fun setVolume(newVolume: Float) {
        volume = newVolume
    }
}

class Metronome(metronomeState: MetronomeState) : Runnable{
    private var isPlaying = true
    private var state = metronomeState
    private var handler = Handler(Looper.getMainLooper())
    private var soundID = 0

    init {
        handler.post(this)
    }

    fun toggle() {
        isPlaying = !isPlaying
    }

    fun setQuarterVolume(volume: Float) {
        state.setVolume(volume)
    }

    fun generateSoundIDs() {
        soundID = state.soundPool.load(appContext, R.raw.beep, 1)
    }

    override fun run() {
        handler.postDelayed(this, state.interval - 1)
        if (isPlaying) {
            state.soundPool.play(soundID, state.volume, state.volume, 0, 0, 2F)
        } else {
            state.soundPool.play(soundID, 0F, 0F, 0, 0, 2F)
        }
    }
}
