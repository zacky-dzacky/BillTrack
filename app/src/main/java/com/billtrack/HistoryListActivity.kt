package com.billtrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billtrack.data.local.AppDatabase
import com.billtrack.data.local.dao.ExpenseDao
import com.billtrack.databinding.ActivityHistoryListBinding
import com.billtrack.ui.historylist.HistoryListAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryListBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var historyListAdapter: HistoryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Expense History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        expenseDao = AppDatabase.getDatabase(applicationContext).expenseDao()

        setupRecyclerView()
        observeHistory()
    }

    private fun setupRecyclerView() {
        historyListAdapter = HistoryListAdapter { expenseRecord ->
            // Navigate to HistoryDetailActivity
            val intent = Intent(this, HistoryDetailActivity::class.java).apply {
                putExtra(HistoryDetailActivity.EXTRA_RECORD_ID, expenseRecord.id)
            }
            startActivity(intent)
        }
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryListActivity)
            adapter = historyListAdapter
        }
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            expenseDao.getAllExpenses().collectLatest { records ->
                if (records.isEmpty()) {
                    binding.historyRecyclerView.visibility = View.GONE
                    binding.emptyHistoryTextView.visibility = View.VISIBLE
                } else {
                    binding.historyRecyclerView.visibility = View.VISIBLE
                    binding.emptyHistoryTextView.visibility = View.GONE
                    historyListAdapter.submitList(records)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
