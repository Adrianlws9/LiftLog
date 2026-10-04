package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.databinding.ItemCustomExerciseRowBinding

class CustomExerciseAdapter(
    private val names: List<String>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<CustomExerciseAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCustomExerciseRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomExerciseRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = names.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.customExerciseNameText.text = names[position]
        holder.binding.removeCustomExerciseButton.setOnClickListener {
            onRemove(position)
        }
    }
}