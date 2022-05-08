package dynamicmetronome.mainactivity

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dynamicmetronome.mainactivity.databinding.*
import dynamicmetronome.gui.CustomAdapter
import dynamicmetronome.gui.ProgramRecyclerModel
import dynamicmetronome.metronome.Metronome
import java.io.File
import java.io.IOException
import java.io.ObjectOutputStream


const val MIN_TEMPO = 20f
const val MAX_TEMPO = 500f
const val STARTING_TEMPO = 130
const val BEATS_PER_MEASURE = 4.0
const val STARTING_QUARTER_VOLUME = .33F

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var programsActivity: ActivityProgramsBinding
    private lateinit var createProgramActivity: CreateProgramBinding
    private lateinit var createPopup: CreatePopupBinding
    private lateinit var popup: PopupWindow
    private lateinit var programEntry: ProgramEntryBinding
    private lateinit var mainMetronome: Metronome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        programsActivity = ActivityProgramsBinding.inflate(layoutInflater)
        createProgramActivity = CreateProgramBinding.inflate(layoutInflater)
        createPopup = CreatePopupBinding.inflate(layoutInflater)
        programEntry = ProgramEntryBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)
        mainMetronome = Metronome(createProgramActivity.graph, applicationContext, R.raw.beep)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  // @todo Find a way to make the orientation not break things.
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
        mainActivity.Tempo.setOnValueChangedListener { _: NumberPicker, _: Int, tempo: Int ->
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
        mainActivity.QuarterVolume.progress = (100 * STARTING_QUARTER_VOLUME).toInt()
        mainActivity.TempoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mainMetronome.tempo = (progress * (MAX_TEMPO - MIN_TEMPO) / 100F + MIN_TEMPO).toInt()
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
        programsActivity.HomeButton.setOnClickListener{
            mainMetronome.stop()
            mainMetronome.program.clear()
            setContentView(mainActivity.root)
        }
        programsActivity.NewProgramButton.setOnClickListener{
            if (mainMetronome.program.name.isNotEmpty()) {
                createProgramActivity.ProgramName.setText(mainMetronome.program.name.substring(0..mainMetronome.program.name.length - 5))
            }
            if (mainMetronome.program.instructions.isEmpty()) {
                createProgramActivity.graph.viewport?.setMaxX(1.0)
                createProgramActivity.graph.viewport?.setMinX(0.0)
                createProgramActivity.graph.viewport?.setMaxY(2.0)
                createProgramActivity.graph.viewport?.setMinY(1.0)
                createProgramActivity.graph.removeAllSeries()
            }
            setContentView(createProgramActivity.root)
        }
        createProgramActivity.graph.viewport?.isXAxisBoundsManual = true
        createProgramActivity.graph.viewport?.isYAxisBoundsManual = true
        createProgramActivity.graph.viewport?.setMaxX(1.0)
        createProgramActivity.graph.viewport?.setMinX(0.0)
        createProgramActivity.graph.viewport?.setMaxY(2.0)
        createProgramActivity.graph.viewport?.setMinY(1.0)
        createProgramActivity.graph.removeAllSeries()
        programsActivity.ProgramList.layoutManager = LinearLayoutManager(this)
        val files: ArrayList<out File>? = applicationContext.filesDir.listFiles()?.toCollection(ArrayList())
        val data = ArrayList<ProgramRecyclerModel>()
        if (files != null) {
            for (file in files) {
                data.add(ProgramRecyclerModel(file.name))
            }
        }
        val adapter = CustomAdapter(data, applicationContext, mainMetronome)
        programsActivity.ProgramList.adapter = adapter
    }

    private fun buildCreateProgramScreenGUI() {
        // Set up the graph
        createProgramActivity.graph.viewport?.setMaxX(1.0)
        createProgramActivity.graph.viewport?.setMinX(0.0)
        createProgramActivity.graph.viewport?.setMaxY(2.0)
        createProgramActivity.graph.viewport?.setMinY(1.0)
        createProgramActivity.graph.removeAllSeries()

        // Set up the new element button's actions
        createProgramActivity.NewElementButton.setOnClickListener{
            popup = PopupWindow(createPopup.root, (Resources.getSystem().displayMetrics.widthPixels * .8).toInt(), (Resources.getSystem().displayMetrics.heightPixels * .8).toInt(), true)
            popup.showAtLocation(programsActivity.root, Gravity.CENTER, 0, 0)
        }

        // Set up the confirm button's actions
        createProgramActivity.ConfirmButton.setOnClickListener{
            // Save the program to the appropriate file
            if (createProgramActivity.ProgramName.text.toString() != "") {
                try {
                    val file = ObjectOutputStream(applicationContext.openFileOutput(createProgramActivity.ProgramName.text.toString() + ".met", Context.MODE_PRIVATE))
                    file.writeObject(mainMetronome.program)
                    file.close()
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, "Failed to save file!", Toast.LENGTH_SHORT).show()
                }
            }
            mainMetronome.stop()
            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createProgramActivity.ProgramName.setText("")
            createPopup.Interpolate.isChecked = false
            createProgramActivity.graph.removeAllSeries()
            mainMetronome.program.clear()
            setContentView(programsActivity.root)
            buildProgramsScreenGUI()
        }

        createProgramActivity.ExecuteProgram.setOnClickListener{
            // Compile and execute the program on the main metronome
            if (!mainMetronome.playing) {
                mainMetronome.executeProgram()
            }
            else {
                mainMetronome.stop()
            }
        }

        createProgramActivity.CancelButton.setOnClickListener{
            mainMetronome.stop()
            createProgramActivity.graph.removeAllSeries()
            createPopup.BarNumber.setText("")
            createPopup.Tempo.setText("")
            createProgramActivity.ProgramName.setText("")
            createPopup.Interpolate.isChecked = false
            mainMetronome.program.clear()
            setContentView(programsActivity.root)
        }
    }

    private fun buildPopupGUI() {
        createPopup.ConfirmButton.setOnClickListener {
            try {
                if (mainMetronome.program.instructions.isEmpty()) {
                    mainMetronome.program.addOrChangeInstruction(0, Integer.parseInt(createPopup.Tempo.text.toString()), false)
                    mainMetronome.program.addOrChangeInstruction(Integer.parseInt(createPopup.BarNumber.text.toString()), Integer.parseInt(createPopup.Tempo.text.toString()), false)
                } else {
                    if (Integer.parseInt(createPopup.BarNumber.text.toString()) >= 0) {
                        mainMetronome.program.addOrChangeInstruction(Integer.parseInt(createPopup.BarNumber.text.toString()), Integer.parseInt(createPopup.Tempo.text.toString()), createPopup.Interpolate.isChecked)
                    } else {
                        Toast.makeText(applicationContext, "The bar number cannot be less than 0!", Toast.LENGTH_SHORT).show()
                    }
                }
                /**@todo Handle inputs that create straight lines*/
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Some inputs are missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                popup.dismiss()
            } catch (e: UninitializedPropertyAccessException) {
            }
            createProgramActivity.graph.removeAllSeries()
            val graphArray = mutableListOf<DataPoint>()
            var tempo = 0
            val instructions = mainMetronome.program.instructions.toSortedMap().toList()
            for (i in instructions) {
                if (!i.second.interpolate) {
                    graphArray.add(DataPoint(i.first.toDouble(), tempo.toDouble()))
                }
                graphArray.add(DataPoint(i.first.toDouble(), i.second.tempo.toDouble()))
                tempo = i.second.tempo
            }
            createProgramActivity.graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))
            if (mainMetronome.program.numBars != 0) {
                createProgramActivity.graph.viewport.setMaxX(mainMetronome.program.numBars.toDouble())
            }
            mainMetronome.formatGraph()
        }
    }
}
