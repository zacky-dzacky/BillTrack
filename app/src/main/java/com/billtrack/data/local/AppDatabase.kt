package com.billtrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.billtrack.data.local.dao.BudgetGoalDao // Added import
import com.billtrack.data.local.dao.ExpenseDao
import com.billtrack.data.local.model.BudgetGoal // Added import
import com.billtrack.data.local.model.ExpenseRecord

@Database(entities = [ExpenseRecord::class, BudgetGoal::class], version = 2, exportSchema = false) // Added BudgetGoal and incremented version
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao // Added DAO accessor

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billtrack_database" // Name of the database file
                )
                .fallbackToDestructiveMigration() // Added fallback migration strategy
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
