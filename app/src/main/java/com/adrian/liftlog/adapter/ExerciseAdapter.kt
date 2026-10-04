package com.adrian.liftlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.adrian.liftlog.databinding.ItemExerciseBinding
import com.adrian.liftlog.databinding.ItemSetRowBinding
import com.adrian.liftlog.model.LoggedExercise
import com.adrian.liftlog.model.LoggedSet
import com.adrian.liftlog.model.WeightUnit
import androidx.core.widget.doAfterTextChanged
import com.adrian.liftlog.R
import com.adrian.liftlog.model.displayString
import com.adrian.liftlog.model.PersonalBest

/**
 * Feeds the list of LoggedExercise into the RecyclerView on the
 * Workout screen. Each exercise gets a card (item_exercise.xml)
 * containing one row (item_set_row.xml) per logged set.
 */
class ExerciseAdapter(
    private val exercises: List<LoggedExercise>,
    private val lastPerformances: Map<String, List<LoggedSet>> = emptyMap(),
    private val personalBests: Map<String, PersonalBest> = emptyMap(),
    private val onRenameExercise: ((position: Int, newName: String) -> Unit)? = null
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    private val unitLabels = arrayOf("KG", "LB", "BAR ONLY")

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun getItemCount(): Int = exercises.size

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.binding.exerciseNameText.text = exercise.exerciseName

        if (onRenameExercise != null) {
            holder.binding.exerciseNameText.setOnClickListener {
                val context = holder.binding.root.context
                val input = android.widget.EditText(context)
                input.setText(exercise.exerciseName)
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Rename exercise")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty()) {
                            onRenameExercise.invoke(position, newName)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val lastSets = lastPerformances[exercise.exerciseName]
        if (lastSets != null && lastSets.isNotEmpty()) {
            holder.binding.lastTimeLabel.visibility = android.view.View.VISIBLE
            holder.binding.lastTimeSetsText.visibility = android.view.View.VISIBLE
            holder.binding.lastTimeSetsText.text = lastSets.joinToString("  •  ") { it.displayString() }
        } else {
            holder.binding.lastTimeLabel.visibility = android.view.View.GONE
            holder.binding.lastTimeSetsText.visibility = android.view.View.GONE
        }

        renderSets(holder.binding, exercise, lastSets)

        holder.binding.addSetButton.setOnClickListener {
            val newSet = if (exercise.sets.isNotEmpty()) {
                // Repeat your most recently entered set — the common case (same weight/reps).
                exercise.sets.last().copy()
            } else {
                // First set of this exercise today — start from what you did last time, if known.
                val fromHistory = lastSets?.getOrNull(0)
                if (fromHistory != null) {
                    LoggedSet(weight = fromHistory.weight, unit = fromHistory.unit, reps = fromHistory.reps)
                } else {
                    LoggedSet(weight = null, unit = WeightUnit.KG, reps = 0)
                }
            }
            exercise.sets.add(newSet)
            renderSets(holder.binding, exercise, lastSets)
        }
    }

    /** Rebuilds the list of set rows inside one exercise card from scratch. */
    private fun renderSets(
        binding: ItemExerciseBinding,
        exercise: LoggedExercise,
        lastSets: List<LoggedSet>?
    ) {
        binding.setsContainer.removeAllViews()

        for ((index, set) in exercise.sets.withIndex()) {
            val rowBinding = ItemSetRowBinding.inflate(
                LayoutInflater.from(binding.root.context), binding.setsContainer, false
            )

            rowBinding.setNumberText.text = (index + 1).toString()

            // Set up the unit dropdown.
            val spinnerAdapter = ArrayAdapter(
                binding.root.context,
                R.layout.spinner_item_selected,
                unitLabels
            )
            spinnerAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
            rowBinding.unitSpinner.adapter = spinnerAdapter
            rowBinding.unitSpinner.setSelection(set.unit.ordinal)

            // Pre-fill weight/reps if already entered.
            rowBinding.weightInput.setText(set.weight?.toString() ?: "")
            rowBinding.repsInput.setText(if (set.reps > 0) set.reps.toString() else "")
            rowBinding.weightInput.isEnabled = set.unit != WeightUnit.BAR_ONLY

            rowBinding.unitSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long
                ) {
                    val newUnit = WeightUnit.entries[pos]
                    exercise.sets[index] = exercise.sets[index].copy(unit = newUnit)
                    rowBinding.weightInput.isEnabled = newUnit != WeightUnit.BAR_ONLY
                    if (newUnit == WeightUnit.BAR_ONLY) {
                        rowBinding.weightInput.setText("")
                    }
                    val best = personalBests[exercise.exerciseName]
                    showSetDiff(rowBinding, exercise.sets[index], lastSets?.getOrNull(index), best)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            rowBinding.weightInput.doAfterTextChanged { text ->
                val value = text?.toString()?.toDoubleOrNull()
                exercise.sets[index] = exercise.sets[index].copy(weight = value)
            }

            rowBinding.repsInput.doAfterTextChanged { text ->
                val value = text?.toString()?.toIntOrNull() ?: 0
                exercise.sets[index] = exercise.sets[index].copy(reps = value)
            }

            rowBinding.weightInput.setOnEditorActionListener { view, _, _ ->
                view.clearFocus()
                val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                true
            }

            rowBinding.repsInput.setOnEditorActionListener { view, _, _ ->
                view.clearFocus()
                val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                true
            }

            rowBinding.removeSetButton.setOnClickListener {
                exercise.sets.removeAt(index)
                renderSets(binding, exercise, lastSets)
            }
            val lastSet = lastSets?.getOrNull(index)
            val best = personalBests[exercise.exerciseName]
            showSetDiff(rowBinding, set, lastSet, best)

            binding.setsContainer.addView(rowBinding.root)
        }
    }

    private fun showSetDiff(
        rowBinding: ItemSetRowBinding,
        currentSet: LoggedSet,
        lastSet: LoggedSet?,
        best: PersonalBest?
    ) {
        val isPr = checkIsPersonalRecord(currentSet, best)

        val repsComparableToLast = lastSet != null && currentSet.unit == lastSet.unit
        val parts = mutableListOf<String>()
        var anyDecrease = false
        var anyChange = false

        if (lastSet != null &&
            currentSet.unit == lastSet.unit &&
            currentSet.unit != WeightUnit.BAR_ONLY &&
            currentSet.weight != null && lastSet.weight != null
        ) {
            val weightDiff = currentSet.weight - lastSet.weight
            if (weightDiff != 0.0) {
                anyChange = true
                val sign = if (weightDiff > 0) "+" else ""
                parts.add("$sign${formatDiffNumber(weightDiff)} ${unitSuffix(currentSet.unit)}")
                if (weightDiff < 0) anyDecrease = true
            }
        }

        if (repsComparableToLast) {
            val repsDiff = currentSet.reps - lastSet!!.reps
            if (repsDiff != 0) {
                anyChange = true
                val sign = if (repsDiff > 0) "+" else ""
                parts.add("$sign$repsDiff reps")
                if (repsDiff < 0) anyDecrease = true
            }
        }

        val baseText = when {
            lastSet == null -> null
            !repsComparableToLast && parts.isEmpty() -> null
            !anyChange -> "Same as last time"
            else -> parts.joinToString(", ") + " vs last time"
        }

        val finalText = when {
            isPr && baseText != null -> "🏆 PR! $baseText"
            isPr -> "🏆 New PR!"
            else -> baseText
        }

        if (finalText == null) {
            rowBinding.setDiffText.visibility = android.view.View.GONE
        } else {
            rowBinding.setDiffText.visibility = android.view.View.VISIBLE
            rowBinding.setDiffText.text = finalText
            rowBinding.setDiffText.setTextColor(
                when {
                    isPr -> android.graphics.Color.parseColor("#E9A23B")
                    anyDecrease -> android.graphics.Color.parseColor("#9AA6B8")
                    else -> android.graphics.Color.parseColor("#7FB685")
                }
            )
        }
    }

    private fun checkIsPersonalRecord(currentSet: LoggedSet, best: PersonalBest?): Boolean {
        if (best == null) return false
        if (currentSet.unit == WeightUnit.KG && currentSet.weight != null) {
            if (best.maxWeightKg == null || currentSet.weight > best.maxWeightKg) return true
        }
        if (currentSet.unit == WeightUnit.LB && currentSet.weight != null) {
            if (best.maxWeightLb == null || currentSet.weight > best.maxWeightLb) return true
        }
        if (best.maxReps == null || currentSet.reps > best.maxReps) {
            if (currentSet.reps > 0) return true
        }
        return false
    }

    private fun formatDiffNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    private fun unitSuffix(unit: WeightUnit): String = if (unit == WeightUnit.KG) "kg" else "lb"
}