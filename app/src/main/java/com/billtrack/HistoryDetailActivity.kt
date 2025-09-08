package com.billtrack

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.billtrack.data.local.AppDatabase
import com.billtrack.data.local.dao.ExpenseDao
import com.billtrack.data.local.model.ExpenseRecord
import com.billtrack.databinding.ActivityHistoryDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryDetailBinding
    private lateinit var expenseDao: ExpenseDao
    private var recordId: Int = -1

    companion object {
        const val EXTRA_RECORD_ID = "com.billtrack.EXTRA_RECORD_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Expense Detail"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        expenseDao = AppDatabase.getDatabase(applicationContext).expenseDao()
        recordId = intent.getIntExtra(EXTRA_RECORD_ID, -1)

        if (recordId == -1) {
            Toast.makeText(this, "Error: Record ID not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadExpenseDetails()
    }

    private fun loadExpenseDetails() {
        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                expenseDao.getExpenseById(recordId)
            }
            if (record != null) {
                displayRecord(record)
            } else {
                Toast.makeText(this@HistoryDetailActivity, "Error: Could not load record details.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayRecord(record: ExpenseRecord) {
        binding.detailAmountTextView.text = record.selectedAmountText
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        binding.detailDateTextView.text = dateFormatter.format(Date(record.timestamp))

        // Load image
        val imageFile = File(record.imageFilePath)
        if (imageFile.exists()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                withContext(Dispatchers.Main) {
                    binding.detailBillImageView.setImageBitmap(bitmap)
                }
            }
        } else {
            binding.detailBillImageView.setImageResource(R.drawable.ic_image_not_found) // Placeholder if image not found
            Toast.makeText(this, "Image not found at ${record.imageFilePath}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
