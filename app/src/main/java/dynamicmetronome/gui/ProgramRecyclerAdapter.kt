package dynamicmetronome.gui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dynamicmetronome.activities.R
import dynamicmetronome.activities.mainMetronome
import dynamicmetronome.metronome.Program
import java.io.FileInputStream
import java.io.ObjectInputStream
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class ProgramRecyclerAdapter(
    private val modelList: ArrayList<ProgramListData>,
    applicationContext: Context,
) : RecyclerView.Adapter<ProgramRecyclerAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.ProgramNameView)
        val delete: Button = itemView.findViewById(R.id.Delete)
        val play: FloatingActionButton = itemView.findViewById(R.id.StartProgram)
    }

    private val path = Path(applicationContext.filesDir.path + "/Programs/").toString()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.program_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = modelList[position]

        holder.nameView.text = itemsViewModel.name

        holder.delete.setOnClickListener {
            var index = 0
            for (i in 0..modelList.size) {
                if (holder.nameView.text == modelList[i].name) {
                    index = i
                    break
                }
            }
            modelList.removeAt(index)
            this.notifyItemRemoved(index)
            Path(path + holder.nameView.text.toString() + ".met").deleteIfExists()
        }

        holder.play.setOnClickListener {
            val name = holder.nameView.text.toString()
            if (mainMetronome.getProgram()?.name != name) {
                mainMetronome.setProgram(ObjectInputStream(FileInputStream("$path/$name.met")).readObject() as Program)
            }
            if (mainMetronome.playing) {
                holder.play.setImageResource(android.R.drawable.ic_media_play)
                mainMetronome.stop()
            } else {
                mainMetronome.setOnStopCallback{ holder.play.setImageResource(android.R.drawable.ic_media_play) }
                holder.play.setImageResource(android.R.drawable.ic_media_pause)
                mainMetronome.start()
            }
        }
    }

    override fun getItemCount(): Int {
        return modelList.size
    }
}