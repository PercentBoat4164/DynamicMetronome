package dynamicmetronome.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dynamicmetronome.activities.databinding.ListProgramsActivityBinding
import dynamicmetronome.gui.ProgramListData
import dynamicmetronome.gui.ProgramRecyclerAdapter
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

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
        buildRecycler()
    }

    override fun onPause() {
        super.onPause()
        callback = mainMetronome.stopCallback
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        buildRecycler()
    }

    override fun onResume() {
        super.onResume()
        mainMetronome.stopCallback = callback
    }

    private fun buildRecycler() {
        val path = Path(applicationContext.filesDir.path + "/Programs/")
        if (path.notExists()) path.createDirectory()
        val files = File(path.toString()).listFiles()?.toCollection(ArrayList())
        val data = ArrayList<ProgramListData>()
        for (file in files!!) data.add(ProgramListData(file.name.substring(0, file.name.length - 4)))
        val adapter = ProgramRecyclerAdapter(data, this)
        programsActivity.ProgramList.adapter = adapter
    }
}
