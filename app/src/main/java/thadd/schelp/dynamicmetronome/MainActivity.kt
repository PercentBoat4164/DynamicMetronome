package thadd.schelp.dynamicmetronome

import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import thadd.schelp.dynamicmetronome.gui.CustomAdapter
import thadd.schelp.dynamicmetronome.gui.ProgramRecyclerModel
import thadd.schelp.dynamicmetronome.databinding.*
import thadd.schelp.dynamicmetronome.metronome.Metronome
import thadd.schelp.dynamicmetronome.metronome.Program

const val MIN_TEMPO = 20f
const val MAX_TEMPO = 500f
const val STARTING_TEMPO = 130
const val BEATS_PER_MEASURE = 4.0

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var programsActivity: ActivityProgramsBinding
    private lateinit var createProgramActivity: CreateProgramBinding
    private lateinit var createPopup: CreatePopupBinding
    private lateinit var popup: PopupWindow

    private var programs = mutableListOf<String>()
    private var mainMetronome = Metronome()

    private val program = Program()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        programsActivity = ActivityProgramsBinding.inflate(layoutInflater)
        createProgramActivity = CreateProgramBinding.inflate(layoutInflater)
        createPopup = CreatePopupBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)
        mainMetronome.setSoundID(applicationContext, R.raw.beep)
        buildHomeScreenGUI()
        buildProgramsScreenGUI()
        buildCreateProgramScreenGUI()
        buildPopupGUI()
    }

    private fun buildHomeScreenGUI() {
        mainActivity.Tempo.minValue = MIN_TEMPO.toInt()
        mainActivity.Tempo.maxValue = MAX_TEMPO.toInt()
        mainActivity.Tempo.value = STARTING_TEMPO
        mainActivity.Tempo.wrapSelectorWheel = false
        mainActivity.Tempo.displayedValues = Array(MAX_TEMPO.toInt()){(it + MIN_TEMPO.toInt()).toString()}
        mainActivity.Tempo.setOnValueChangedListener {
                _: NumberPicker, _: Int, tempo: Int ->
            mainMetronome.tempo = tempo
            mainActivity.TempoSeekbar.progress = (tempo.toFloat() / MAX_TEMPO * 100).toInt()
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
                mainMetronome.volume = progress / 100F
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.QuarterVolume.progress = 100
        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mainMetronome.tempo = (progress * (MAX_TEMPO - MIN_TEMPO) / 100f + MIN_TEMPO).toInt()
                    mainActivity.Tempo.value = mainMetronome.tempo
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        mainActivity.TempoSeekbar.progress = (STARTING_TEMPO.toFloat() / (MAX_TEMPO) * 100).toInt()
        mainActivity.ProgramsButton.setOnClickListener{
            mainMetronome.stop()
            setContentView(programsActivity.root)
        }
    }

    private fun buildProgramsScreenGUI() {
        programsActivity.HomeButton.setOnClickListener{ setContentView(mainActivity.root) }
        programsActivity.NewProgramButton.setOnClickListener{ setContentView(createProgramActivity.root) }
//        var i = 0
//        val file = File(filesDir.name, "names.txt")
//        Log.d("------LOG------", if(file.exists()) {"exists"} else {"does not exist"})
//        file.createNewFile()
//        val filenames = file.readText().split("\n")
        val filenames = arrayListOf("This", "is", "different!")
        programsActivity.ProgramList.layoutManager = LinearLayoutManager(this)
        val data = ArrayList<ProgramRecyclerModel>()
        for (filename in filenames) {
            programs.add(filename)
            data.add(ProgramRecyclerModel(filename))
        }
        val adapter = CustomAdapter(data)
        programsActivity.ProgramList.adapter = adapter
    }

    private fun buildCreateProgramScreenGUI() {
        // Set up the graph
        createProgramActivity.graph.viewport?.isXAxisBoundsManual = true
        createProgramActivity.graph.viewport?.isYAxisBoundsManual = true
        createProgramActivity.graph.removeAllSeries()

        // Set up the new element button's actions
        createProgramActivity.NewElementButton.setOnClickListener{
            popup = PopupWindow(createPopup.root, (Resources.getSystem().displayMetrics.widthPixels * .8).toInt(), (Resources.getSystem().displayMetrics.heightPixels * .8).toInt(), true)
            popup.showAtLocation(programsActivity.root, Gravity.CENTER, 0, 0)
        }

        // Set up the confirm button's actions
        createProgramActivity.ConfirmButton.setOnClickListener{
            // Save the program to the appropriate file
            createProgramActivity.ProgramName.text.toString()
//            program.getData()

            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createPopup.Interpolate.isChecked = false
            setContentView(programsActivity.root)
            if (!programs.contains(createProgramActivity.ProgramName.text.toString() + ".met")) {
                programs.add(createProgramActivity.ProgramName.text.toString() + ".met")
            }
        }

        createProgramActivity.ExecuteProgram.setOnClickListener{
            // Compile and execute the program on the main metronome
            mainMetronome.executeProgram(program.compile(), createProgramActivity.graph)
        }

        createProgramActivity.CancelButton.setOnClickListener{
            createProgramActivity.graph.removeAllSeries()
            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createPopup.Interpolate.isChecked = false
            program.instructions = mutableMapOf()
            setContentView(programsActivity.root)
        }
    }

    private fun buildPopupGUI() {
        createPopup.ConfirmButton.setOnClickListener{
            try {
                if (program.instructions.isEmpty()) {
                    program.addOrChangeInstruction(1, Integer.parseInt(createPopup.Tempo.text.toString()), false)
                }
                else {
                    program.addOrChangeInstruction(Integer.parseInt(createPopup.BarNumber.text.toString()), Integer.parseInt(createPopup.Tempo.text.toString()), createPopup.Interpolate.isChecked)
                }
                if (Integer.parseInt(createPopup.Tempo.text.toString()) == 0) {
                    program.instructions.remove(Integer.parseInt(createPopup.BarNumber.text.toString()))
                }
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
            val instructions = program.instructions.toSortedMap().toList()
            for (i in instructions) {
                if (!i.second.interpolate) {
                    graphArray.add(DataPoint(i.first.toDouble(), tempo.toDouble()))
                }
                graphArray.add(DataPoint(i.first.toDouble(), i.second.tempo.toDouble()))
                tempo = i.second.tempo
            }
            createProgramActivity.graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))
            if (program.highestTempo != program.lowestTempo) {
                createProgramActivity.graph.viewport.setMinX(1.0)
                createProgramActivity.graph.viewport.setMaxX(program.numBars.toDouble())
                createProgramActivity.graph.viewport.setMinY(program.lowestTempo - ((program.lowestTempo + program.highestTempo) / 2 - program.lowestTempo))
                createProgramActivity.graph.viewport.setMaxY(program.highestTempo)
            }
        }
    }
}
