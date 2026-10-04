package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.model.DefaultWorkoutPlan
import com.adrian.liftlog.data.PlanDayEntity
import com.adrian.liftlog.databinding.ItemPlanDayBinding

class PlanDayAdapter(
    private val days: List<PlanDayEntity>,
    private val onDayClick: (PlanDayEntity) -> Unit
) : RecyclerView.Adapter<PlanDayAdapter.PlanDayViewHolder>() {

    inner class PlanDayViewHolder(val binding: ItemPlanDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanDayViewHolder {
        val binding = ItemPlanDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlanDayViewHolder(binding)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: PlanDayViewHolder, position: Int) {
        val day = days[position]
        holder.binding.planDayNameText.text = DefaultWorkoutPlan.dayNameFor(day.dayOfWeek)
        holder.binding.planDayTitleText.text = day.title
        holder.binding.root.setOnClickListener { onDayClick(day) }
    }
}