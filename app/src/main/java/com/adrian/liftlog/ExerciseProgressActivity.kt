package com.adrian.liftlog

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.adrian.liftlog.databinding.ActivityExerciseProgressBinding
import com.adrian.liftlog.ui.ExerciseProgressViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExerciseProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseProgressBinding
    private lateinit var viewModel: ExerciseProgressViewModel
    private var exerciseNames: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExerciseProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.progressToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Exercise Progress"

        viewModel = ViewModelProvider(this)[ExerciseProgressViewModel::class.java]

        viewModel.exerciseNames.observe(this) { names ->
            exerciseNames = names
            val spinnerAdapter = ArrayAdapter(
                this, R.layout.spinner_item_selected, names
            )
            spinnerAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
            binding.exercisePickerSpinner.adapter = spinnerAdapter

            binding.exercisePickerSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                    if (names.isNotEmpty()) {
                        viewModel.loadProgress(names[pos])
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        viewModel.progress.observe(this) { points ->
            if (points.size < 2) {
                binding.progressChart.visibility = android.view.View.GONE
                binding.noDataText.visibility = android.view.View.VISIBLE
            } else {
                binding.progressChart.visibility = android.view.View.VISIBLE
                binding.noDataText.visibility = android.view.View.GONE

                val values = points.map { it.second.toFloat() }
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val labels = points.map { dateFormat.format(Date(it.first)) }
                binding.progressChart.setData(values, labels)
            }
        }

        viewModel.loadExerciseNames()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}