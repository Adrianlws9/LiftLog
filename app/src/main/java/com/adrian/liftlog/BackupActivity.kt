package com.adrian.liftlog

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.adrian.liftlog.databinding.ActivityBackupBinding
import com.adrian.liftlog.ui.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding
    private lateinit var viewModel: BackupViewModel
    private var pendingExportJson: String? = null

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray())
            }
            Toast.makeText(this, "Export saved", Toast.LENGTH_SHORT).show()
        }
        pendingExportJson = null
    }

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val json = contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
            if (json != null) {
                confirmImport(json)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.backupToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Backup & Restore"

        viewModel = ViewModelProvider(this)[BackupViewModel::class.java]

        binding.exportButton.setOnClickListener { startExport() }
        binding.importButton.setOnClickListener { startImport() }
    }

    private fun startExport() {
        viewModel.exportData { json ->
            pendingExportJson = json
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            createFileLauncher.launch("liftlog_backup_$dateStr.json")
        }
    }

    private fun startImport() {
        openFileLauncher.launch("application/json")
    }

    private fun confirmImport(json: String) {
        AlertDialog.Builder(this)
            .setTitle("Import this file?")
            .setMessage("This will add all workouts from the file into your current data. Only do this on a fresh install to avoid duplicates.")
            .setPositiveButton("Import") { _, _ ->
                viewModel.importData(json) { result ->
                    result.onSuccess { (workouts, exercises) ->
                        Toast.makeText(
                            this, "Imported $workouts workouts, $exercises exercises", Toast.LENGTH_LONG
                        ).show()
                    }
                    result.onFailure {
                        Toast.makeText(this, "Import failed: invalid file", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}