package com.adrian.liftlog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adrian.liftlog.adapter.PlanDayAdapter
import com.adrian.liftlog.databinding.ActivityPlanBinding
import com.adrian.liftlog.ui.PlanViewModel

class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private lateinit var viewModel: PlanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.planToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PlanViewModel::class.java]
        binding.planDaysRecyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.days.observe(this) { days ->
            binding.planDaysRecyclerView.adapter = PlanDayAdapter(days) { day ->
                val intent = Intent(this, PlanDayActivity::class.java)
                intent.putExtra("day_of_week", day.dayOfWeek)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDays()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}