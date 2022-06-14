package dynamicmetronome.gui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dynamicmetronome.metronome.Metronome
import dynamicmetronome.metronome.Program
import dynamicmetronome.mainactivity.R
import java.io.ObjectInputStream



class CustomAdapter (private val modelList: ArrayList<ProgramRecyclerModel>, private val applicationContext: Context, private var metronome: Metronome) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = this.itemView.findViewById(R.id.ProgramNameView)
        val delete: Button = this.itemView.findViewById(R.id.Delete)
        val play: FloatingActionButton = this.itemView.findViewById(R.id.StartProgram)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.program_entry, parent, false)
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
            applicationContext.deleteFile(holder.nameView.text.toString() + ".met")
        }
        holder.play.setOnClickListener {
            if (!metronome.playing) {
                val file = ObjectInputStream(applicationContext.openFileInput(holder.nameView.text.toString() + ".met"))
                metronome.program = (file.readObject() as Program)
                metronome.program.name = holder.nameView.text.toString()
                metronome.executeProgram()
            }
            else {
                metronome.stop()
            }
        }
    }

    override fun getItemCount(): Int {
        return modelList.size
    }
}