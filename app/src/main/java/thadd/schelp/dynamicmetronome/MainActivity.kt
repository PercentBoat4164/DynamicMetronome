package thadd.schelp.dynamicmetronome

import android.content.res.Resources
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import thadd.schelp.dynamicmetronome.databinding.*
import java.io.File
import java.lang.NumberFormatException

const val MIN_TEMPO = 20
const val MAX_TEMPO = 300F
const val STARTING_TEMPO = 200
const val BEATS_PER_MEASURE = 4

/*
Currently following tutorial at:
https://www.raywenderlich.com/1560485-android-recyclerview-tutorial-with-kotlin
in order to master the recycler layout which is needed for displaying a list of programs.
 */





class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var programsActivity: ActivityProgramsBinding
    private lateinit var createProgramActivity: CreateProgramBinding
    private lateinit var createPopup: CreatePopupBinding
    private var metronome = Metronome(MetronomeState(), this)
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

        metronome.generateSoundIDs()

        metronomeProgram = MetronomeProgram(metronome, this)

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
        mainActivity.Tempo.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int -> metronome.state.setTempo(tempo); mainActivity.TempoSeekbar.progress = (tempo.toFloat() / MAX_TEMPO * 100).toInt() }

        mainActivity.QuarterMute.setOnCheckedChangeListener{ _: CompoundButton, isChecked: Boolean -> if (isChecked) { metronome.setVolume(0F)} else { metronome.setVolume(mainActivity.QuarterVolume.progress / 100F) } }

        mainActivity.StartStopButton.setOnClickListener{ metronome.togglePlaying() }

        mainActivity.QuarterVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                metronome.setVolume(progress / 100F)
                mainActivity.QuarterMute.isChecked = false
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = 100

        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    metronome.state.setTempo((progress * ((MAX_TEMPO - MIN_TEMPO) / 100)).toInt() + MIN_TEMPO)
                    mainActivity.Tempo.value = metronome.state.tempo
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
            metronomeProgram.states = mutableMapOf()
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

//class ProgramsRecyclerAdapter(private val programs: ArrayList<MetronomeProgram>) : RecyclerView.Adapter<ProgramsRecyclerAdapter.ProgramHolder>() {
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramHolder {
//        val inflatedView = parent.inflate(R.layout.program_entry, false)
//        return ProgramHolder(inflatedView)
//    }
//
//    override fun onBindViewHolder(holder: ProgramHolder, position: Int) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getItemCount() = programs.size
//
//    class ProgramHolder(private val view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
//        private var program: MetronomeProgram? = null
//        init { view.setOnClickListener(this) }
//
//        override fun onClick(v: View?) {
//            TODO("Not yet implemented")
//        }
//
//        companion object {
//            private val PROGRAM_KEY = "PROGRAM"
//        }
//    }
//}
