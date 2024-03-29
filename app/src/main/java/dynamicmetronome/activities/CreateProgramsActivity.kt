package dynamicmetronome.activities

import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import dynamicmetronome.activities.databinding.CreateProgramsActivityBinding
import dynamicmetronome.activities.databinding.EditorPopupBinding
import dynamicmetronome.metronome.Program
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

class CreateProgramsActivity : Activity() {
    private lateinit var createProgramActivity: CreateProgramsActivityBinding
    private lateinit var editorPopup: EditorPopupBinding
    private lateinit var popup: PopupWindow
    private var program = Program()
    private var stopWhenExit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editorPopup = DataBindingUtil.setContentView(this, R.layout.editor_popup)
        createProgramActivity = DataBindingUtil.setContentView(this, R.layout.create_programs_activity)

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
                    val file = ObjectOutputStream(FileOutputStream(
                        File(applicationContext.filesDir.toString() + "/Programs/" + createProgramActivity.ProgramName.text.toString() + ".met"),
                        false
                    ))
                    file.writeObject(program)
                    file.close()
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, "Failed to save file!", Toast.LENGTH_SHORT)
                        .show()
                }
                exit()
            } else Toast.makeText(applicationContext, "Program does not have a name.", Toast.LENGTH_SHORT).show()
        }

        createProgramActivity.ExecuteProgram.setOnClickListener {
            if (mainMetronome.playing) {
                mainMetronome.stop()
            } else {
                createProgramActivity.ExecuteProgram.setImageResource(android.R.drawable.ic_media_pause)
                mainMetronome.setProgram(program)
                mainMetronome.start()
            }
        }

        createProgramActivity.CancelButton.setOnClickListener {
            exit()
        }

        /** Popup window initialization*/
        editorPopup.ConfirmButton.setOnClickListener {
            try {
                program.addOrChangeInstruction(
                    editorPopup.BarNumberInputField.text.toString().toLong(),
                    editorPopup.TempoInputField.text.toString().trim().split("\\s+".toRegex())[0].toDoubleOrNull()!!,
                    editorPopup.Interpolate.isChecked
                )
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Some inputs are missing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } catch (e: NegativeArraySizeException) {
                Toast.makeText(this, "Bar cannot be less than 1.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            popup.dismiss()
            // Rebuild the graph
            createProgramActivity.ProgramDisplay.setProgram(program)
        }
        createProgramActivity.ProgramDisplay.setProgram(program)
        createProgramActivity.ProgramDisplay.resetPlayHead()
    }

    override fun onResume() {
        super.onResume()
        // Synchronize with mainMetronome
        if (mainMetronome.playing) {
            stopWhenExit = false
            createProgramActivity.ExecuteProgram.setImageResource(android.R.drawable.ic_media_pause)
        }
        var callback = mainMetronome.stopCallback
        mainMetronome.stopCallback = {
            callback()
            callback = {}
            stopWhenExit = true
            createProgramActivity.ExecuteProgram.setImageResource(android.R.drawable.ic_media_play)
            createProgramActivity.ProgramDisplay.resetPlayHead()
        }
        if (mainMetronome.getProgram() != null) program = mainMetronome.getProgram()!!
        mainMetronome.clickCallback = { playHead: Float -> createProgramActivity.ProgramDisplay.setPlayHead(playHead) }
        val programName = program.name
        if (programName.isNotEmpty()) createProgramActivity.ProgramName.setText(programName)
    }

    private fun exit() {
        if (stopWhenExit) {
            mainMetronome.stop()
            mainMetronome.setProgram(null)
        }
        editorPopup.BarNumberInputField.setText("")
        editorPopup.TempoInputField.setText("")
        finish()
    }
}