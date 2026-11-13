package com.adrian.liftlog

import android.Manifest
import android.app.AlarmManager
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.ExerciseAdapter
import com.adrian.liftlog.databinding.ActivityWorkoutBinding
import com.adrian.liftlog.databinding.DialogWorkoutNotesBinding
import com.adrian.liftlog.model.LoggedExercise
import com.adrian.liftlog.model.PersonalBest
import com.adrian.liftlog.model.formatClock
import com.adrian.liftlog.service.BreakTimerService
import com.adrian.liftlog.session.ActiveWorkoutSession
import com.adrian.liftlog.ui.EditWorkoutViewModel
import com.adrian.liftlog.ui.WorkoutViewModel

class WorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutBinding
    private lateinit var adapter: ExerciseAdapter
    private lateinit var workoutViewModel: WorkoutViewModel
    private lateinit var editViewModel: EditWorkoutViewModel

    private var isEditMode = false
    private var editWorkoutId: Long = -1L
    private val editExercises = mutableListOf<LoggedExercise>()
    private var editNotes: String? = null
    private var editTitle: String = "Workout"

    private var personalBests: Map<String, PersonalBest> = emptyMap()

    /** Logging mode uses the app-wide session; edit mode uses its own local list. */
    private val loggedExercises: MutableList<LoggedExercise>
        get() = if (isEditMode) editExercises else ActiveWorkoutSession.exercises

    private var currentNotes: String?
        get() = if (isEditMode) editNotes else ActiveWorkoutSession.notes
        set(value) {
            if (isEditMode) editNotes = value else ActiveWorkoutSession.notes = value
        }

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            if (!isEditMode && ActiveWorkoutSession.elapsedSeconds() % 15 == 0L) {
                ActiveWorkoutSession.saveDraft()
            }
            timerHandler.postDelayed(this, 1000)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.workoutToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleLeave()
        })

        workoutViewModel = ViewModelProvider(this)[WorkoutViewModel::class.java]
        editViewModel = ViewModelProvider(this)[EditWorkoutViewModel::class.java]

        editWorkoutId = intent.getLongExtra("edit_workout_id", -1L)
        isEditMode = editWorkoutId != -1L

        if (isEditMode) {
            editTitle = intent.getStringExtra("workout_title") ?: "Workout"
            binding.workoutScreenTitle.text = editTitle
            setUpAdapter()
            setUpEditMode()
            return
        }

        val resume = intent.getBooleanExtra("resume_session", false)
        when {
            resume -> {
                if (ActiveWorkoutSession.isActive) beginLogging(fresh = false) else finish()
            }
            ActiveWorkoutSession.isActive -> askResumeOrStartNew()
            else -> {
                startFreshSession()
                beginLogging(fresh = true)
            }
        }
    }

    // ---------- Session handling ----------

    private fun startFreshSession() {
        ActiveWorkoutSession.start(
            dayName = intent.getStringExtra("day_name") ?: "UNKNOWN",
            title = intent.getStringExtra("workout_title") ?: "Workout",
            exerciseNames = (intent.getStringArrayExtra("exercise_names") ?: arrayOf()).toList()
        )
    }

    private fun askResumeOrStartNew() {
        AlertDialog.Builder(this)
            .setTitle("Workout already in progress")
            .setMessage("\"${ActiveWorkoutSession.title}\" is still running. Resume it, or discard it and start this one?")
            .setCancelable(false)
            .setPositiveButton("Resume it") { _, _ -> beginLogging(fresh = false) }
            .setNegativeButton("Discard & start new") { _, _ ->
                startFreshSession()
                beginLogging(fresh = true)
            }
            .setNeutralButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun beginLogging(fresh: Boolean) {
        binding.workoutScreenTitle.text = ActiveWorkoutSession.title
        setUpAdapter()
        setUpLoggingMode(fresh)
    }

    private fun setUpAdapter() {
        adapter = ExerciseAdapter(loggedExercises)
        binding.exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exerciseRecyclerView.adapter = adapter
    }

    // ---------- Logging mode ----------

    private fun setUpLoggingMode(fresh: Boolean) {
        val names = loggedExercises.map { it.exerciseName }

        workoutViewModel.lastPerformances.observe(this) { performances ->
            adapter = ExerciseAdapter(loggedExercises, performances, personalBests)
            binding.exerciseRecyclerView.adapter = adapter
        }
        workoutViewModel.personalBests.observe(this) { bests ->
            personalBests = bests
            adapter = ExerciseAdapter(
                loggedExercises,
                workoutViewModel.lastPerformances.value ?: emptyMap(),
                personalBests
            )
            binding.exerciseRecyclerView.adapter = adapter
        }
        workoutViewModel.loadLastPerformances(names, excludeWorkoutId = -1L)
        workoutViewModel.loadPersonalBests(names)

        if (fresh) {
            workoutViewModel.previousNotes.observe(this) { prev ->
                if (ActiveWorkoutSession.notes == null) ActiveWorkoutSession.notes = prev
            }
            workoutViewModel.loadPreviousNotes(ActiveWorkoutSession.title)
        }

        binding.finishWorkoutButton.text = "FINISH"
        binding.finishWorkoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Finish workout?")
                .setMessage("This will save your workout and end the session.")
                .setPositiveButton("Finish") { _, _ ->
                    binding.finishWorkoutButton.isEnabled = false
                    stopTimer()
                    workoutViewModel.saveWorkout(
                        ActiveWorkoutSession.dayName,
                        ActiveWorkoutSession.title,
                        ActiveWorkoutSession.exercises,
                        ActiveWorkoutSession.elapsedSeconds(),
                        currentNotes
                    ) {
                        ActiveWorkoutSession.clear()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.breakButton.setOnClickListener { showBreakDurationPicker() }
        binding.cancelBreakButton.setOnClickListener {
            stopService(Intent(this, BreakTimerService::class.java))
        }
        BreakTimerService.remainingSeconds.observe(this) { updateBreakCard(it) }

        updateTimerDisplay()
        startTimer()
        askNotificationPermissionIfNeeded()
        ensureExactAlarmPermission()
    }

    // ---------- Edit mode (unchanged behaviour) ----------

    private fun setUpEditMode() {
        binding.workoutTimerText.visibility = android.view.View.GONE
        binding.breakButton.visibility = android.view.View.GONE

        editNotes = intent.getStringExtra("existing_notes")

        binding.finishWorkoutButton.text = "SAVE"
        binding.finishWorkoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Save changes?")
                .setMessage("This will overwrite the original logged workout.")
                .setPositiveButton("Save") { _, _ ->
                    binding.finishWorkoutButton.isEnabled = false
                    editViewModel.saveEdits(
                        editWorkoutId, editTitle, editNotes, editExercises
                    ) { finish() }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        editViewModel.existingExercises.observe(this) { exercises ->
            editExercises.clear()
            editExercises.addAll(exercises)
            adapter = ExerciseAdapter(
                editExercises,
                onRenameExercise = { position, newName ->
                    editExercises[position] = editExercises[position].copy(exerciseName = newName)
                    adapter.notifyItemChanged(position)
                }
            )
            binding.exerciseRecyclerView.adapter = adapter
        }
        editViewModel.loadWorkout(editWorkoutId)
    }

    // ---------- Leaving the screen ----------

    private fun handleLeave() {
        if (isEditMode) {
            AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Your edits haven't been saved.")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton("Keep going", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Leave workout screen?")
            .setMessage("Keep it running and tap the bar at the bottom of any screen to come back, or discard everything logged so far.")
            .setPositiveButton("Keep running") { _, _ -> finish() }
            .setNegativeButton("Discard workout") { _, _ ->
                stopTimer()
                ActiveWorkoutSession.clear()
                finish()
            }
            .setNeutralButton("Stay", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        handleLeave()
        return true
    }

    // ---------- Notes dialog ----------

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

    // ---------- Break timer UI ----------

    private fun updateBreakCard(remaining: Int?) {
        if (remaining == null) {
            binding.breakTimerCard.visibility = android.view.View.GONE
            return
        }
        binding.breakTimerCard.visibility = android.view.View.VISIBLE
        binding.breakTimerText.text = String.format("%d:%02d", remaining / 60, remaining % 60)

        val total = BreakTimerService.totalSeconds.value ?: remaining
        binding.breakProgressIndicator.progress =
            if (total > 0) ((total - remaining).toFloat() / total * 100).toInt() else 0
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

    // ---------- Workout timer (display only; the clock lives in the session) ----------

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            timerHandler.post(timerRunnable)
        }
    }

    private fun stopTimer() {
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerDisplay() {
        if (!isEditMode) {
            binding.workoutTimerText.text = formatClock(ActiveWorkoutSession.elapsedSeconds())
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isEditMode) ActiveWorkoutSession.saveDraft()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer() // stops the on-screen ticking only; the session keeps its start time
    }
}