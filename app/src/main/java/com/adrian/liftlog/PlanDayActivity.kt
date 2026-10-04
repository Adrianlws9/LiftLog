package com.adrian.liftlog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.PlanExerciseAdapter
import com.adrian.liftlog.data.PlanExerciseEntity
import com.adrian.liftlog.databinding.ActivityPlanDayBinding
import com.adrian.liftlog.model.DefaultWorkoutPlan
import com.adrian.liftlog.ui.PlanDayViewModel
import java.util.Calendar

class PlanDayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanDayBinding
    private lateinit var viewModel: PlanDayViewModel
    private var dayOfWeek: Int = -1
    private var currentExercises: List<PlanExerciseEntity> = emptyList()
    private var suppressAutoSave = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlanDayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.planDayToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dayOfWeek = intent.getIntExtra("day_of_week", -1)
        title = DefaultWorkoutPlan.dayNameFor(dayOfWeek)

        viewModel = ViewModelProvider(this)[PlanDayViewModel::class.java]
        binding.planExercisesRecyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.day.observe(this) { day ->
            if (day != null) {
                suppressAutoSave = true
                binding.dayTitleInput.setText(day.title)
                binding.restDaySwitch.isChecked = day.isRestDay
                suppressAutoSave = false
            }
        }

        viewModel.exercises.observe(this) { exercises ->
            currentExercises = exercises
            renderExerciseList()
        }

        binding.restDaySwitch.setOnCheckedChangeListener { _, _ -> saveDayDetails() }
        binding.dayTitleInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveDayDetails()
        }

        binding.addExerciseButton.setOnClickListener { showAddExerciseDialog() }

        viewModel.load(dayOfWeek)
    }

    private fun saveDayDetails() {
        if (suppressAutoSave) return
        val title = binding.dayTitleInput.text.toString().trim()
        if (title.isEmpty()) return
        viewModel.saveDayDetails(title, binding.restDaySwitch.isChecked)
    }

    private fun renderExerciseList() {
        binding.planExercisesRecyclerView.adapter = PlanExerciseAdapter(
            exercises = currentExercises,
            onRename = { exercise -> showRenameDialog(exercise) },
            onMoveUp = { position -> moveWithinDay(position, position - 1) },
            onMoveDown = { position -> moveWithinDay(position, position + 1) },
            onMoveDay = { exercise -> showMoveDayDialog(exercise) },
            onDelete = { exercise -> confirmDelete(exercise) }
        )
    }

    private fun moveWithinDay(fromIndex: Int, toIndex: Int) {
        if (toIndex < 0 || toIndex >= currentExercises.size) return
        val mutable = currentExercises.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        viewModel.reorder(mutable.map { it.id })
    }

    private fun showAddExerciseDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Exercise name"
        AlertDialog.Builder(this)
            .setTitle("Add exercise")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.addExercise(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(exercise: PlanExerciseEntity) {
        val input = android.widget.EditText(this)
        input.setText(exercise.name)
        AlertDialog.Builder(this)
            .setTitle("Rename exercise")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.renameExercise(exercise.id, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(exercise: PlanExerciseEntity) {
        AlertDialog.Builder(this)
            .setTitle("Remove exercise?")
            .setMessage("${exercise.name} will be removed from this day's plan.")
            .setPositiveButton("Remove") { _, _ -> viewModel.deleteExercise(exercise.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveDayDialog(exercise: PlanExerciseEntity) {
        val dayValues = intArrayOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        val dayLabels = dayValues.map { DefaultWorkoutPlan.dayNameFor(it) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Move to day")
            .setItems(dayLabels) { _, index ->
                val newDay = dayValues[index]
                if (newDay != dayOfWeek) {
                    viewModel.moveExerciseToDay(exercise.id, newDay)
                }
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onPause() {
        super.onPause()
        saveDayDetails()
    }
}