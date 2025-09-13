package com.billtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.billtrack.data.local.model.ExpenseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expenseRecord: ExpenseRecord): Long // Returns the new rowId

    @Query("SELECT * FROM expense_records ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expense_records WHERE id = :id")
    suspend fun getExpenseById(id: Int): ExpenseRecord?

    @Query("SELECT * FROM expense_records")
    suspend fun getAllTotalExpenses(): List<ExpenseRecord>

    // You can add other methods here later, e.g.:
    // @Query("DELETE FROM expense_records WHERE id = :id")
    // suspend fun deleteExpenseById(id: Int)
}
