package com.billtrack.data.local.impl

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.billtrack.data.local.AppDatabase
import com.billtrack.data.local.dao.ExpenseDao
import com.billtrack.data.local.dao.ExpenseInteface
import com.billtrack.data.local.model.ExpenseRecord
import com.billtrack.utils.BillTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseDaoImpl(applicationContext: Context) : ExpenseInteface {
    private val expenseDao: ExpenseDao = AppDatabase.getDatabase(applicationContext).expenseDao()

    override fun insertExpense(expenseRecord: ExpenseRecord): Long {
        TODO("Not yet implemented")
    }

    override fun getAllExpenses(): List<ExpenseRecord> {
        TODO("Not yet implemented")
    }

    override fun getExpenseById(id: Int): ExpenseRecord? {
        TODO("Not yet implemented")
    }

    suspend override fun getAllTotalExpenses(): String {
        val records = expenseDao.getAllTotalExpenses()

        var total = 0.0
        for (record in records) {
            try {
                total += BillTextParser.convertFormattedStringToInt(record.selectedAmountText) ?: 0.0
            } catch (e: Exception) {
                continue
            }
        }
        return BillTextParser.formatNumberWithSeparator(total);
    }
}