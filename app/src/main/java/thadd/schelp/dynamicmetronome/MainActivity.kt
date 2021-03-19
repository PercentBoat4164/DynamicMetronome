package thadd.schelp.dynamicmetronome

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import thadd.schelp.dynamicmetronome.databinding.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.IndexOutOfBoundsException
import java.lang.NumberFormatException
import java.util.zip.Inflater

const val MIN_TEMPO = 20
const val MAX_TEMPO = 300F
const val STARTING_TEMPO = 200
const val BEATS_PER_MEASURE = 4

@SuppressLint("StaticFieldLeak")
lateinit var appContext: Context

/*

https://www.raywenderlich.com/1560485-android-recyclerview-tutorial-with-kotlin

 */





class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var programsActivity: ActivityProgramsBinding
    private lateinit var createProgramActivity: CreateProgramBinding
    private lateinit var createPopup: CreatePopupBinding
    private var metronomeState = MetronomeState()
    private var metronome = Metronome(metronomeState)
    private lateinit var metronomeProgram: MetronomeProgram
    private lateinit var popup: PopupWindow
    private var programs = mutableListOf<String>()
    private lateinit var programsArrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        programsArrayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, programs)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        programsActivity = ActivityProgramsBinding.inflate(layoutInflater)
        createProgramActivity = CreateProgramBinding.inflate(layoutInflater)
        createPopup = CreatePopupBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        appContext = applicationContext

        metronome.generateSoundIDs()

        metronomeProgram = MetronomeProgram()

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

        var i = 0
        File(filesDir, "/").walk().forEach { if (i != 0) { programs.add(it.name) }; i++ }
//        programsActivity.ListView.adapter = programsArrayAdapter
//        programsActivity.ListView.setOnItemClickListener{
//            parent: AdapterView<*>, _: View, position: Int, _: Long ->
//            val selectedProgram = parent.getItemAtPosition(position)
//            program.load(selectedProgram.toString().dropLast(4))
//            setContentView(createProgramActivity.root)
//            createProgramActivity.ProgramName.setText(selectedProgram.toString().dropLast(4))
//            createPopup.BarNumber.setText("0")
//            createPopup.Tempo.setText(program.states[0]?.tempo.toString())
//            createPopup.Interpolate.isChecked = program.states[0]?.interpolate == true
//            createPopup.ConfirmButton.callOnClick()
//        }
    }

    private fun buildCreateProgramScreenGUI() {
        createProgramActivity.graph.viewport.isScrollable = true
        createProgramActivity.graph.viewport.isScalable = true

        createProgramActivity.NewElementButton.setOnClickListener{
            popup = PopupWindow(createPopup.root, (Resources.getSystem().displayMetrics.widthPixels * .9).toInt(), (Resources.getSystem().displayMetrics.heightPixels * .9).toInt(), true)
            popup.showAtLocation(programsActivity.root, Gravity.CENTER, 0, 0)
        }

        createProgramActivity.ConfirmButton.setOnClickListener{
            metronomeProgram.save(createProgramActivity.ProgramName.text.toString())
            createProgramActivity.graph.removeAllSeries()
            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createPopup.Interpolate.isChecked = false
            setContentView(programsActivity.root)
            if (!programs.contains(createProgramActivity.ProgramName.text.toString() + ".met")) {
                programs.add(createProgramActivity.ProgramName.text.toString() +".met")
                programsArrayAdapter.notifyDataSetChanged()
            }
        }

        createProgramActivity.ExecuteProgram.setOnClickListener{ metronomeProgram.compile().execute() }

        createProgramActivity.CancelButton.setOnClickListener{
            createProgramActivity.graph.removeAllSeries()
            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createPopup.Interpolate.isChecked = false
            metronomeProgram.states = mutableMapOf()
            setContentView(programsActivity.root)
        }
    }

    private fun buildPopupGUI() {
        createPopup.ConfirmButton.setOnClickListener{
            try {
                if (metronomeProgram.states.isEmpty()) { metronomeProgram.addOrChangeInstruction(0, Integer.parseInt(createPopup.Tempo.text.toString()), false) }
                else { metronomeProgram.addOrChangeInstruction(Integer.parseInt(createPopup.BarNumber.text.toString()), Integer.parseInt(createPopup.Tempo.text.toString()), createPopup.Interpolate.isChecked) }
                if (Integer.parseInt(createPopup.Tempo.text.toString()) == 0) { metronomeProgram.states.remove(Integer.parseInt(createPopup.BarNumber.text.toString())) }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Some inputs are missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                popup.dismiss()
            } catch (e: UninitializedPropertyAccessException) {}
            createProgramActivity.graph.removeAllSeries()
            val graphArray = mutableListOf<DataPoint>()
            var tempo = 0
            val instructions = metronomeProgram.states.toSortedMap().toList()
            for (i in instructions) {
                if (!i.second.interpolate) { graphArray.add(DataPoint(i.first.toDouble() + 1, tempo.toDouble())) }
                graphArray.add(DataPoint(i.first.toDouble() + 1, i.second.tempo.toDouble()))
                tempo = i.second.tempo
            }
            createProgramActivity.graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))
            createProgramActivity.graph.viewport.setMinY(0.0)
            createProgramActivity.graph.viewport.setMaxY(instructions[instructions.size - 1].second.tempo.toDouble())
            createProgramActivity.graph.viewport.setMinX(1.0)
            createProgramActivity.graph.viewport.setMaxX((instructions[instructions.size - 1].first.toDouble() + 1).coerceAtLeast(8.0))
        }
    }
}

class ProgramRecyclerAdapter(private val programs: ArrayList<MetronomeProgram>) : RecyclerView.Adapter<ProgramRecyclerAdapter.ProgramHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ProgramHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount() = programs.size

    class ProgramHolder(private val view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private var program: MetronomeProgram? = null
        init { view.setOnClickListener(this) }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
        }

        companion object {
            private val PROGRAM_KEY = "PROGRAM"
        }
    }
}

class MetronomeProgram : Runnable {
    var states = mutableMapOf<Int, MetronomeState>()
    private var compiled = mutableListOf<Long>()
    private var handler = Handler(Looper.getMainLooper())
    private var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build()).build()
    private var soundID = soundPool.load(appContext, R.raw.beep, 1)
    private var playHead = 0
    private var volume = 1F

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): MetronomeProgram {
        states[(bar - 1).coerceAtLeast(0)] = MetronomeState().setTempo(tempo).setInterpolation(interpolate)
        return this
    }

    fun compile(): MetronomeProgram{
        compiled = mutableListOf()
        val instructions = states.toSortedMap().toList()
        var tempo = instructions[0].second.tempo
        var count = 0
        for (instruction in instructions.indices) {
            tempo = instructions[instruction].second.tempo
            if (tempo <= 0) { break }
            try {
                for (barNumber in (instructions[instruction].first + count)..instructions[instruction + 1].first) {
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
            count ++
        }
        compiled.add((60000 / tempo).toLong())
        return this
    }

    fun execute() {
        handler.post(this)
    }

    fun save(filename: String): MetronomeProgram {
        var byteArray = byteArrayOf()
        val instructions = states.toSortedMap().toList()
        for (instruction in instructions) {
            byteArray += instruction.first.toByte()
            byteArray += instruction.second.tempo.toByte()
            byteArray += (if (instruction.second.interpolate) 1 else 0).toByte()
        }
        val file = File(appContext.filesDir, "$filename.met")
        try {
            file.delete()
        } catch (e: FileNotFoundException) {}
        file.createNewFile()
        file.writeBytes(byteArray)
        return this
    }

    fun load(filename: String): MetronomeProgram {
        states = mutableMapOf()
        try {
            val byteArray = File(appContext.filesDir, "$filename.met").readBytes()
            for (byte in byteArray.indices step 3) { states[byteArray[byte].toInt()] = MetronomeState().setTempo(byteArray[byte + 1].toInt()).setInterpolation(byteArray[byte + 2].toInt() == 1) }
        } catch (e:FileNotFoundException) {}
        return this
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
        //handler.post(this)
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
