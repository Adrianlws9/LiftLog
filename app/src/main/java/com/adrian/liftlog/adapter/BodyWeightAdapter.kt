package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.data.BodyWeightEntity
import com.adrian.liftlog.databinding.ItemBodyWeightRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BodyWeightAdapter(
    private val entries: List<BodyWeightEntity>,
    private val onDelete: (BodyWeightEntity) -> Unit
) : RecyclerView.Adapter<BodyWeightAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())

    inner class ViewHolder(val binding: ItemBodyWeightRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBodyWeightRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val unitLabel = entry.unit.lowercase()
        val weightStr = if (entry.weight == entry.weight.toLong().toDouble()) {
            entry.weight.toLong().toString()
        } else entry.weight.toString()
        holder.binding.bodyWeightValueText.text = "$weightStr $unitLabel"
        holder.binding.bodyWeightDateText.text = dateFormat.format(Date(entry.dateEpochMillis))

        holder.binding.deleteBodyWeightButton.setOnClickListener {
            onDelete(entry)
        }
    }
}