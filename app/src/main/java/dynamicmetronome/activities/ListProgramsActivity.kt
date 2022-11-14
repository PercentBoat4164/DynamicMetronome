package dynamicmetronome.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dynamicmetronome.activities.databinding.ListProgramsActivityBinding
import dynamicmetronome.gui.ProgramListData
import dynamicmetronome.gui.ProgramRecyclerAdapter

class ListProgramsActivity : Activity() {
    private lateinit var programsActivity: ListProgramsActivityBinding

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
        buildRecycler()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        buildRecycler()
    }

    private fun buildRecycler() {
        val files = applicationContext.filesDir.listFiles()?.toCollection(ArrayList())
        val data = ArrayList<ProgramListData>()
        for (file in files!!) data.add(ProgramListData(file.name.substring(0, file.name.length - 4)))
        val adapter = ProgramRecyclerAdapter(data, this, mainMetronome)
        programsActivity.ProgramList.adapter = adapter
    }
}
