package com.adrian.liftlog

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.adrian.liftlog.databinding.ActivityMainBinding
import com.adrian.liftlog.model.DefaultWorkoutPlan
import com.adrian.liftlog.ui.PlanDayViewModel
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PlanDayViewModel
    private var todaysDayOfWeek: Int = -1
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.mainToolbar)
        title = "LiftLog"

        drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.mainToolbar,
            androidx.appcompat.R.string.abc_action_bar_up_description,
            androidx.appcompat.R.string.abc_action_bar_up_description
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerItemClick(menuItem)
        }

        viewModel = ViewModelProvider(this)[PlanDayViewModel::class.java]
        todaysDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        viewModel.day.observe(this) { day ->
            binding.loadingSpinner.visibility = android.view.View.GONE
            binding.mainContentScroll.visibility = android.view.View.VISIBLE
            if (day != null) {
                binding.dayNameText.text = DefaultWorkoutPlan.dayNameFor(day.dayOfWeek)
                binding.workoutTitleText.text = day.title
                binding.startWorkoutButton.visibility =
                    if (day.isRestDay) android.view.View.GONE else android.view.View.VISIBLE
            }
        }

        viewModel.exercises.observe(this) { exercises ->
            binding.exerciseListContainer.removeAllViews()
            for (exercise in exercises) {
                addExerciseRow(exercise.name)
            }
            binding.startWorkoutButton.setOnClickListener {
                startWorkout(exercises.map { it.name })
            }
        }
    }

    private fun handleDrawerItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.nav_plan -> startActivity(Intent(this, PlanActivity::class.java))
            R.id.nav_custom -> startActivity(Intent(this, CustomWorkoutActivity::class.java))
            R.id.nav_summary -> startActivity(Intent(this, SummaryActivity::class.java))
            R.id.nav_backup -> startActivity(Intent(this, BackupActivity::class.java))
            R.id.nav_bodyweight -> startActivity(Intent(this, BodyWeightActivity::class.java))
            R.id.nav_progress -> startActivity(Intent(this, ExerciseProgressActivity::class.java))
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(todaysDayOfWeek)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    private fun addExerciseRow(exerciseName: String) {
        val row = TextView(this).apply {
            text = exerciseName
            textSize = 18f
            setPadding(0, 24, 0, 24)
            gravity = Gravity.START
            typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@MainActivity, R.font.bricolage_grotesque
            )
        }
        binding.exerciseListContainer.addView(row)
    }

    private fun startWorkout(exerciseNames: List<String>) {
        val dayName = DefaultWorkoutPlan.dayNameFor(todaysDayOfWeek)
        val title = binding.workoutTitleText.text.toString()

        val intent = Intent(this, WorkoutActivity::class.java)
        intent.putExtra("day_name", dayName)
        intent.putExtra("workout_title", title)
        intent.putExtra("exercise_names", exerciseNames.toTypedArray())
        startActivity(intent)
    }
}