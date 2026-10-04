package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.data.WorkoutEntity
import com.adrian.liftlog.databinding.ItemHistoryRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.adrian.liftlog.model.formatDuration

class HistoryAdapter(
    private val workouts: List<WorkoutEntity>,
    private val onWorkoutClick: (WorkoutEntity) -> Unit,
    private val onDeleteClick: (WorkoutEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())

    inner class HistoryViewHolder(val binding: ItemHistoryRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = workouts.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val workout = workouts[position]
        holder.binding.historyDateText.text = dateFormat.format(Date(workout.dateEpochMillis))
        holder.binding.historyTitleText.text = workout.title

        if (workout.durationSeconds != null) {
            holder.binding.historyDurationText.text = formatDuration(workout.durationSeconds)
            holder.binding.historyDurationText.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.historyDurationText.visibility = android.view.View.GONE
        }

        holder.binding.root.setOnClickListener {
            onWorkoutClick(workout)
        }

        holder.binding.deleteHistoryButton.setOnClickListener {
            onDeleteClick(workout)
        }
    }
}