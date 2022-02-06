package thadd.schelp.dynamicmetronome.gui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import thadd.schelp.dynamicmetronome.R

class CustomAdapter (private val modelList: List<ProgramRecyclerModel>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) { val nameView: TextView = itemView.findViewById(
        R.id.ProgramNameView
    ) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.program_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = modelList[position]
        holder.nameView.text = itemsViewModel.text
    }

    override fun getItemCount(): Int { return modelList.size }
}