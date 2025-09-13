package com.billtrack.data.local.dao

import com.billtrack.data.local.model.ExpenseRecord

interface ExpenseInteface {
    fun insertExpense(expenseRecord: ExpenseRecord): Long // Returns the new rowId
    fun getAllExpenses(): List<ExpenseRecord>
    fun getExpenseById(id: Int): ExpenseRecord?
    suspend fun getAllTotalExpenses(): String
}