package thadd.schelp.dynamicmetronome

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.view.Gravity
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import thadd.schelp.dynamicmetronome.databinding.*
import java.lang.IndexOutOfBoundsException

const val MIN_TEMPO = 20
const val MAX_TEMPO = 300F
const val STARTING_TEMPO = 200
const val BEATS_PER_MEASURE = 4

@SuppressLint("StaticFieldLeak")
lateinit var appContext: Context

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var programsActivity: ActivityProgramsBinding
    private lateinit var createProgramActivity: CreateProgramBinding
    private lateinit var createPopup: CreatePopupBinding
    private var metronomeState = MetronomeState()
    private var metronome = Metronome(metronomeState)
    private lateinit var program: Program
    private lateinit var popup: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        programsActivity = ActivityProgramsBinding.inflate(layoutInflater)
        createProgramActivity = CreateProgramBinding.inflate(layoutInflater)
        createPopup = CreatePopupBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        appContext = applicationContext

        metronome.generateSoundIDs()

        program = Program()

        buildHomeScreenGUI()
        buildProgramsScreenGUI()
        buildCreateProgramScreenGUI()
        buildPopupGUI()
    }

    private fun buildHomeScreenGUI() {
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
        mainActivity.QuarterVolume.progress = 100

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

        mainActivity.ProgramsButton.setOnClickListener{ setContentView(programsActivity.root);}
    }

    private fun buildProgramsScreenGUI() {
        programsActivity.HomeButton.setOnClickListener{ setContentView(mainActivity.root) }

        programsActivity.NewProgramButton.setOnClickListener{ setContentView(createProgramActivity.root) }
    }

    private fun buildCreateProgramScreenGUI() {
        createProgramActivity.graph.addSeries(LineGraphSeries(arrayOf(DataPoint(0.0, 100.0), DataPoint(5.0, 100.0), DataPoint(5.0, 150.0), DataPoint(10.0, 200.0))))
        createProgramActivity.graph.canScrollHorizontally(5)

        createProgramActivity.NewElementButton.setOnClickListener{
            popup = PopupWindow(createPopup.root, (Resources.getSystem().displayMetrics.widthPixels * .9).toInt(), (Resources.getSystem().displayMetrics.heightPixels * .9).toInt(), true)
            popup.showAtLocation(programsActivity.root, Gravity.CENTER, 0, 0)
            //if (program.states.isEmpty()) { createPopup.BarNumber.setText("Bar Number = 0") }
        }

        createProgramActivity.HomeButton.setOnClickListener{ setContentView(programsActivity.root) }
    }

    private fun buildPopupGUI() {
        createPopup.ConfirmButton.setOnClickListener{
            createProgramActivity.graph.removeAllSeries()
            if (program.states.isEmpty()) {
                program.addOrChangeInstruction(0, Integer.parseInt(createPopup.Tempo.text.toString()), createPopup.Interpolate.isChecked)
            } else {
                program.addOrChangeInstruction(Integer.parseInt(createPopup.BarNumber.text.toString()), Integer.parseInt(createPopup.Tempo.text.toString()), createPopup.Interpolate.isChecked)
            }
            val graphArray = mutableListOf<DataPoint>()
            for (i in program.states.toSortedMap().toList()) { graphArray.plusElement(DataPoint(i.first.toDouble(), i.second.tempo.toDouble())) }
            createProgramActivity.graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))
            popup.dismiss()
        }
    }
}

class Program : Runnable{
    var states = mutableMapOf<Int, MetronomeState>()
    var compiled = mutableListOf<Long>()
    var handler = Handler(Looper.getMainLooper())
    var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build()).build()
    var soundID = soundPool.load(appContext, R.raw.beep, 1)
    var playHead = 0
    var volume = 1F

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): Program {
        states[(bar - 1).coerceAtLeast(0)] = MetronomeState().setTempo(tempo).setInterpolation(interpolate)
        return this
    }

    fun compile(): Program{
        val instructions = states.toSortedMap().toList()
        var tempo = instructions[0].second.tempo
        for (instruction in 0..instructions.size) {
            tempo = instructions[instruction].second.tempo
            try {
                for (barNumber in instructions[instruction].first..instructions[instruction + 1].first) {
                    for (beatNumber in (barNumber * 4) until (barNumber * 4) + BEATS_PER_MEASURE) {
                        if (instructions[instruction + 1].second.interpolate) {
                            tempo += (instructions[instruction + 1].second.tempo - instructions[instruction].second.tempo) / (BEATS_PER_MEASURE * (instructions[instruction + 1].first - instructions[instruction].first))
                            compiled.add((60000 / tempo).toLong())
                        } else {
                            compiled.add((60000 / tempo).toLong())
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                break
            }
        }
        compiled.add((60000 / instructions[instructions.size - 1].second.tempo).toLong())
        return this
    }

    fun execute() {
        handler.post(this)
    }

    override fun run() {
        try {
            handler.postDelayed(this, compiled[playHead])
            playHead++
            soundPool.play(soundID, volume, volume, 0, 0, 2F)
        } catch (e: IndexOutOfBoundsException) {
            playHead = 0
        }
    }
}

class MetronomeState {
    var tempo = STARTING_TEMPO
    var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build()).build()
    var volume = 1F
    var interval = (60000F / tempo).toLong()
    var interpolate = false

    @JvmName("setTempo1")
    fun setTempo(newTempo: Int) : MetronomeState {
        tempo = newTempo
        interval = (60000F / tempo).toLong()
        return this
    }

    @JvmName("setVolume1")
    fun setVolume(newVolume: Float) : MetronomeState {
        volume = newVolume
        return this
    }

    fun setInterpolation(interpolation: Boolean): MetronomeState {
        interpolate = interpolation
        return this
    }
}

class Metronome(metronomeState: MetronomeState) : Runnable {
    private var isPlaying = false
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
