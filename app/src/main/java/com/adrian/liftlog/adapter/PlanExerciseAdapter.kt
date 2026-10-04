package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.data.PlanExerciseEntity
import com.adrian.liftlog.databinding.ItemPlanExerciseRowBinding

class PlanExerciseAdapter(
    private val exercises: List<PlanExerciseEntity>,
    private val onRename: (PlanExerciseEntity) -> Unit,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onMoveDay: (PlanExerciseEntity) -> Unit,
    private val onDelete: (PlanExerciseEntity) -> Unit
) : RecyclerView.Adapter<PlanExerciseAdapter.PlanExerciseViewHolder>() {

    inner class PlanExerciseViewHolder(val binding: ItemPlanExerciseRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanExerciseViewHolder {
        val binding = ItemPlanExerciseRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlanExerciseViewHolder(binding)
    }

    override fun getItemCount(): Int = exercises.size

    override fun onBindViewHolder(holder: PlanExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.binding.exerciseNameText.text = exercise.name

        holder.binding.exerciseNameText.setOnClickListener { onRename(exercise) }
        holder.binding.moveUpButton.setOnClickListener { onMoveUp(position) }
        holder.binding.moveDownButton.setOnClickListener { onMoveDown(position) }
        holder.binding.moveDayButton.setOnClickListener { onMoveDay(exercise) }
        holder.binding.deleteExerciseButton.setOnClickListener { onDelete(exercise) }

        holder.binding.moveUpButton.alpha = if (position == 0) 0.3f else 1f
        holder.binding.moveDownButton.alpha = if (position == exercises.size - 1) 0.3f else 1f
    }
}