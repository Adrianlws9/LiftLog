package com.adrian.liftlog

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.BodyWeightAdapter
import com.adrian.liftlog.databinding.ActivityBodyWeightBinding
import com.adrian.liftlog.ui.BodyWeightViewModel
import com.adrian.liftlog.model.toKg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BodyWeightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBodyWeightBinding
    private lateinit var viewModel: BodyWeightViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBodyWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.bodyWeightToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Body Weight"

        viewModel = ViewModelProvider(this)[BodyWeightViewModel::class.java]
        binding.weightRecyclerView.layoutManager = LinearLayoutManager(this)

        val unitAdapter = ArrayAdapter(
            this, R.layout.spinner_item_selected, arrayOf("KG", "LB")
        )
        unitAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.weightUnitSpinner.adapter = unitAdapter

        viewModel.allEntries.observe(this) { entries ->
            binding.emptyWeightText.visibility =
                if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.weightChart.visibility =
                if (entries.size < 2) android.view.View.GONE else android.view.View.VISIBLE

            if (entries.size >= 2) {
                val chronological = entries.sortedBy { it.dateEpochMillis }
                val values = chronological.map {
                    it.weight.toKg(com.adrian.liftlog.model.WeightUnit.valueOf(it.unit)).toFloat()
                }
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val labels = chronological.map { dateFormat.format(Date(it.dateEpochMillis)) }
                binding.weightChart.setData(values, labels)
            }

            binding.weightRecyclerView.adapter = BodyWeightAdapter(entries) { entry ->
                AlertDialog.Builder(this)
                    .setTitle("Delete entry?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteEntry(entry.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        binding.addWeightButton.setOnClickListener {
            val value = binding.weightInput.text.toString().toDoubleOrNull()
            if (value == null || value <= 0) {
                android.widget.Toast.makeText(this, "Enter a valid weight", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val unit = if (binding.weightUnitSpinner.selectedItemPosition == 0) "KG" else "LB"
            viewModel.addEntry(value, unit)
            binding.weightInput.setText("")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}