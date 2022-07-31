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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editorPopup = DataBindingUtil.setContentView(this, R.layout.editor_popup)
        createProgramActivity = DataBindingUtil.setContentView(this, R.layout.create_programs_activity)

        mainMetronome.graph = createProgramActivity.Graph

        if (mainMetronome.program.name.isNotEmpty()) {
            createProgramActivity.ProgramName.setText(mainMetronome.program.name)
        }

        // Set up the graph
        mainMetronome.updateGraph()

        // Set up the new element button's actions
        createProgramActivity.NewElementButton.setOnClickListener{
            createProgramActivity.ProgramName.clearFocus()
            popup = PopupWindow(editorPopup.root, (Resources.getSystem().displayMetrics.widthPixels * .8).toInt(), (Resources.getSystem().displayMetrics.heightPixels * .8).toInt(), true)
            popup.showAtLocation(createProgramActivity.root, Gravity.CENTER, 0, 0)
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
            exit()
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
            exit()
        }

        /** Popup window initialization*/
        editorPopup.ConfirmButton.setOnClickListener {
            try {
                if (mainMetronome.program.instructions.isEmpty()) {
                    mainMetronome.program.addOrChangeInstruction(0, Integer.parseInt(editorPopup.TempoInputField.text.toString()), false)
                    editorPopup.Interpolate.isChecked = false
                }
                if (Integer.parseInt(editorPopup.BarNumberInputField.text.toString()) >= 0) {
                    mainMetronome.program.addOrChangeInstruction(Integer.parseInt(editorPopup.BarNumberInputField.text.toString()), Integer.parseInt(editorPopup.TempoInputField.text.toString()), editorPopup.Interpolate.isChecked)
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Some inputs are missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            popup.dismiss()
            createProgramActivity.Graph.removeAllSeries()
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
            createProgramActivity.Graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))
            if (mainMetronome.program.numBars != 0) {
                createProgramActivity.Graph.viewport.setMaxX(mainMetronome.program.numBars.toDouble())
            }
            mainMetronome.formatGraph()

            // close popup
            editorPopup.Interpolate.isChecked = false
        }
    }

    private fun exit() {
        mainMetronome.stop()
        mainMetronome.program.clear()
        mainMetronome.graph = null
        createProgramActivity.Graph.removeAllSeries()
        createProgramActivity.ProgramName.setText("")
        editorPopup.BarNumberInputField.setText("")
        editorPopup.TempoInputField.setText("")
        finish()
    }
}