package com.adrian.liftlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.ExerciseAdapter
import com.adrian.liftlog.databinding.ActivityWorkoutBinding
import com.adrian.liftlog.model.LoggedExercise
import com.adrian.liftlog.service.BreakTimerService
import com.adrian.liftlog.ui.EditWorkoutViewModel
import com.adrian.liftlog.ui.WorkoutViewModel
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.ViewGroup
import com.adrian.liftlog.databinding.DialogWorkoutNotesBinding
import androidx.activity.OnBackPressedCallback
import android.app.AlarmManager
import android.provider.Settings

class WorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutBinding
    private val loggedExercises = mutableListOf<LoggedExercise>()
    private lateinit var adapter: ExerciseAdapter

    private lateinit var workoutViewModel: WorkoutViewModel
    private lateinit var editViewModel: EditWorkoutViewModel

    private var isEditMode = false
    private var editWorkoutId: Long = -1L
    private var currentNotes: String? = null

    private lateinit var dayName: String
    private lateinit var workoutTitle: String

    private val timerHandler = Handler(Looper.getMainLooper())
    private var workoutStartTimeMillis = 0L
    private var secondsElapsed = 0L
    private var timerRunning = false

    private var personalBests: Map<String, com.adrian.liftlog.model.PersonalBest> = emptyMap()

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed = (System.currentTimeMillis() - workoutStartTimeMillis) / 1000
            updateTimerDisplay()
            timerHandler.postDelayed(this, 1000)
        }
    }

    private fun ensureExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Allow exact alarms")
                    .setMessage("To make sure your break timer reliably vibrates when it ends, LiftLog needs permission to schedule exact alarms.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Not critical if denied — timer still works, just less visible when minimized. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.workoutToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmDiscard()
            }
        })

        workoutViewModel = ViewModelProvider(this)[WorkoutViewModel::class.java]
        editViewModel = ViewModelProvider(this)[EditWorkoutViewModel::class.java]

        editWorkoutId = intent.getLongExtra("edit_workout_id", -1L)
        isEditMode = editWorkoutId != -1L

        dayName = intent.getStringExtra("day_name") ?: "UNKNOWN"
        workoutTitle = intent.getStringExtra("workout_title") ?: "Workout"
        binding.workoutScreenTitle.text = workoutTitle

        adapter = ExerciseAdapter(loggedExercises)
        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exerciseRecyclerView.adapter = adapter

        if (isEditMode) {
            setUpEditMode()
        } else {
            setUpLoggingMode()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.workout_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_notes) {
            showNotesDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNotesDialog() {
        val dialogBinding = DialogWorkoutNotesBinding.inflate(layoutInflater)
        dialogBinding.notesDialogInput.setText(currentNotes ?: "")

        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 40
        } else {
            dialog.window?.setDimAmount(0.65f)
        }

        dialogBinding.notesDialogDoneButton.setOnClickListener {
            currentNotes = dialogBinding.notesDialogInput.text.toString().trim().ifEmpty { null }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setUpLoggingMode() {
        val exerciseNames = intent.getStringArrayExtra("exercise_names") ?: arrayOf()
        loggedExercises.clear()

        workoutViewModel.previousNotes.observe(this) { prev ->
            if (currentNotes == null) {
                currentNotes = prev
            }
        }
        workoutViewModel.loadPreviousNotes(workoutTitle)

        for (name in exerciseNames) {
            loggedExercises.add(LoggedExercise(exerciseName = name))
        }
        adapter.notifyDataSetChanged()

        workoutViewModel.lastPerformances.observe(this) { performances ->
            adapter = ExerciseAdapter(loggedExercises, performances, personalBests)
            binding.exerciseRecyclerView.adapter = adapter
        }
        workoutViewModel.personalBests.observe(this) { bests ->
            personalBests = bests
            adapter = ExerciseAdapter(loggedExercises, workoutViewModel.lastPerformances.value ?: emptyMap(), personalBests)
            binding.exerciseRecyclerView.adapter = adapter
        }
        workoutViewModel.loadLastPerformances(exerciseNames.toList(), excludeWorkoutId = -1L)
        workoutViewModel.loadPersonalBests(exerciseNames.toList())

        binding.finishWorkoutButton.text = "FINISH"
        binding.finishWorkoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Finish workout?")
                .setMessage("This will save your workout and end the session.")
                .setPositiveButton("Finish") { _, _ ->
                    binding.finishWorkoutButton.isEnabled = false
                    stopTimer()
                    val notes = currentNotes
                    workoutViewModel.saveWorkout(
                        dayName, workoutTitle, loggedExercises, secondsElapsed, notes
                    ) { finish() }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.breakButton.setOnClickListener { showBreakDurationPicker() }
        binding.cancelBreakButton.setOnClickListener {
            stopService(Intent(this, BreakTimerService::class.java))
        }
        BreakTimerService.remainingSeconds.observe(this) { remaining ->
            updateBreakCard(remaining)
        }

        startTimer()
        askNotificationPermissionIfNeeded()
        ensureExactAlarmPermission()
    }

    private fun setUpEditMode() {
        // Editing a past workout — no live full-workout timer or break timer needed.
        binding.workoutTimerText.visibility = android.view.View.GONE
        binding.breakButton.visibility = android.view.View.GONE

        currentNotes = intent.getStringExtra("existing_notes")

        binding.finishWorkoutButton.text = "SAVE"
        binding.finishWorkoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Save changes?")
                .setMessage("This will overwrite the original logged workout.")
                .setPositiveButton("Save") { _, _ ->
                    binding.finishWorkoutButton.isEnabled = false
                    val notes = currentNotes
                    editViewModel.saveEdits(
                        editWorkoutId, workoutTitle, notes, loggedExercises
                    ) { finish() }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        editViewModel.existingExercises.observe(this) { exercises ->
            loggedExercises.clear()
            loggedExercises.addAll(exercises)
            adapter = ExerciseAdapter(
                loggedExercises,
                onRenameExercise = { position, newName ->
                    loggedExercises[position] = loggedExercises[position].copy(exerciseName = newName)
                    adapter.notifyItemChanged(position)
                }
            )
            binding.exerciseRecyclerView.adapter = adapter
        }
        editViewModel.loadWorkout(editWorkoutId)
    }

    private fun updateBreakCard(remaining: Int?) {
        if (remaining == null) {
            binding.breakTimerCard.visibility = android.view.View.GONE
            return
        }
        binding.breakTimerCard.visibility = android.view.View.VISIBLE
        val minutes = remaining / 60
        val seconds = remaining % 60
        binding.breakTimerText.text = String.format("%d:%02d", minutes, seconds)

        val total = BreakTimerService.totalSeconds.value ?: remaining
        val progress = if (total > 0) {
            ((total - remaining).toFloat() / total.toFloat() * 100).toInt()
        } else 0
        binding.breakProgressIndicator.progress = progress
    }

    private fun showBreakDurationPicker() {
        val options = arrayOf("30 seconds", "1 minute", "1 minute 30 seconds")
        val secondsValues = intArrayOf(30, 60, 90)

        AlertDialog.Builder(this)
            .setTitle("Break duration")
            .setItems(options) { _, index -> startBreakTimer(secondsValues[index]) }
            .show()
    }

    private fun startBreakTimer(seconds: Int) {
        val intent = Intent(this, BreakTimerService::class.java)
        intent.putExtra(BreakTimerService.EXTRA_TOTAL_SECONDS, seconds)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun askNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            workoutStartTimeMillis = System.currentTimeMillis()
            timerHandler.post(timerRunnable)
        }
    }

    private fun stopTimer() {
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerDisplay() {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        binding.workoutTimerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onSupportNavigateUp(): Boolean {
        confirmDiscard()
        return true
    }

    private fun confirmDiscard() {
        AlertDialog.Builder(this)
            .setTitle(if (isEditMode) "Discard changes?" else "Discard workout?")
            .setMessage(
                if (isEditMode) "Your edits haven't been saved."
                else "Your logged sets haven't been saved. Leaving now will lose them."
            )
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep going", null)
            .show()
    }
}