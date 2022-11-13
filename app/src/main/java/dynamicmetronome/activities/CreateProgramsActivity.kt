package dynamicmetronome.activities

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dynamicmetronome.activities.databinding.CreateProgramsActivityBinding
import dynamicmetronome.activities.databinding.EditorPopupBinding
import java.io.IOException
import java.io.ObjectOutputStream

class CreateProgramsActivity : Activity() {
    private lateinit var createProgramActivity: CreateProgramsActivityBinding
    private lateinit var editorPopup: EditorPopupBinding
    private lateinit var popup: PopupWindow
    private var series = LineGraphSeries(mutableListOf(DataPoint(0.0, 0.0)).toTypedArray())
    private var playHead = LineGraphSeries(mutableListOf(DataPoint(0.0, 0.0)).toTypedArray())
    private var playHeadLocation = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainMetronome.setOnProgramStartCallback {
            playHeadLocation = 0.0
//            mainMetronome.setOnClickCallback { stepPlayHead() }
        }

        editorPopup = DataBindingUtil.setContentView(this, R.layout.editor_popup)
        createProgramActivity =
            DataBindingUtil.setContentView(this, R.layout.create_programs_activity)

        createProgramActivity.Graph.addSeries(series)
        val programName = mainMetronome.program.name
        if (programName.isNotEmpty()) createProgramActivity.ProgramName.setText(programName)

        // Set up the new element button's actions
        createProgramActivity.NewElementButton.setOnClickListener {
            createProgramActivity.ProgramName.clearFocus()
            popup = PopupWindow(
                editorPopup.root,
                (Resources.getSystem().displayMetrics.widthPixels * .8).toInt(),
                (Resources.getSystem().displayMetrics.heightPixels * .8).toInt(),
                true
            )
            popup.showAtLocation(createProgramActivity.root, Gravity.CENTER, 0, 0)
        }
        // Set up the confirm button's actions
        createProgramActivity.ConfirmButton.setOnClickListener {
            // Save the program to the appropriate file
            if (createProgramActivity.ProgramName.text.toString() != "") {
                try {
                    val file = ObjectOutputStream(
                        applicationContext.openFileOutput(
                            createProgramActivity.ProgramName.text.toString() + ".met",
                            Context.MODE_PRIVATE
                        )
                    )
                    file.writeObject(mainMetronome)
                    file.close()
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, "Failed to save file!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            exit()
        }

        createProgramActivity.ExecuteProgram.setOnClickListener {
            mainMetronome.executeProgram()
        }

        createProgramActivity.CancelButton.setOnClickListener {
            exit()
        }

        /** Popup window initialization*/
        editorPopup.ConfirmButton.setOnClickListener {
            try {
                mainMetronome.program.addOrChangeInstruction(
                    editorPopup.BarNumberInputField.text.toString().toLong(),
                    editorPopup.TempoInputField.text.toString().trim().split("\\s+".toRegex())[0].toDoubleOrNull()!!,
                    editorPopup.Interpolate.isChecked
                )
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Some inputs are missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            popup.dismiss()
            // Rebuild the graph
            buildGraph()
        }
    }

    private fun buildGraph() {
        createProgramActivity.Graph.removeSeries(series)
        series = LineGraphSeries()
        series.dataPointsRadius = 100.0f
        val instructions = mainMetronome.program.getInstructionsAndBars()
        series.appendData(
            DataPoint(0.0,
                instructions[0].second.startTempo),
            false,
            Int.MAX_VALUE,
            true)
        for (i in instructions.indices) {
            try {  // This exception will always be triggered on the first run of this loop.
                // If there is not interpolation and there is a change in tempo.
                if (instructions[i].second.startTempo != instructions[i - 1].second.startTempo &&
                    instructions[i - 1].second.tempoOffset == 0.0)
                    series.appendData(
                        DataPoint(instructions[i - 1].first.toDouble(),
                            instructions[i].second.startTempo),
                        false,
                        Int.MAX_VALUE,
                        true)
            } catch (_: IndexOutOfBoundsException) {}
            series.appendData(
                DataPoint(instructions[i].first.toDouble(), instructions[i].second.startTempo),
                true,
                Int.MAX_VALUE,
                true)
        }
        createProgramActivity.Graph.addSeries(series)
    }

    private fun stepPlayHead() {
        playHeadLocation += 0.25
        createProgramActivity.Graph.removeSeries(playHead)
        playHead = LineGraphSeries(
            mutableListOf(
                DataPoint(playHeadLocation, 0.0),
                DataPoint(
                    playHeadLocation,
                    120.0)).toTypedArray())
        createProgramActivity.Graph.addSeries(playHead)
    }

    private fun exit() {
        mainMetronome.stop()
        mainMetronome.program.clear()
        createProgramActivity.Graph.removeAllSeries()
        createProgramActivity.ProgramName.setText("")
        editorPopup.BarNumberInputField.setText("")
        editorPopup.TempoInputField.setText("")
        finish()
    }
}