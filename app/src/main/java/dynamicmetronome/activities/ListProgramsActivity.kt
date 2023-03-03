package dynamicmetronome.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dynamicmetronome.activities.databinding.ListProgramsActivityBinding
import dynamicmetronome.gui.ProgramRecyclerAdapter

class ListProgramsActivity : Activity() {
    private lateinit var programsActivity: ListProgramsActivityBinding
    private var callback:() -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        programsActivity = DataBindingUtil.setContentView(this, R.layout.list_programs_activity)

        programsActivity.HomeButton.setOnClickListener {
            mainMetronome.stop()
            finish()
        }
        programsActivity.NewProgramButton.setOnClickListener {
            startActivityForResult(Intent(this, CreateProgramsActivity::class.java), 1)
        }

        programsActivity.ProgramList.layoutManager = LinearLayoutManager(this)
        programsActivity.ProgramList.adapter = ProgramRecyclerAdapter(ArrayList(), this)
    }

    override fun onPause() {
        super.onPause()
        callback = mainMetronome.stopCallback
    }

    override fun onResume() {
        super.onResume()
        (programsActivity.ProgramList.adapter as ProgramRecyclerAdapter).build()
        mainMetronome.stopCallback = callback
    }
}
