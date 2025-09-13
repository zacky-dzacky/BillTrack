package com.billtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.billtrack.data.local.model.BudgetGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(budgetGoal: BudgetGoal): Long

    @Query("SELECT * FROM budget_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<BudgetGoal>> // Using Flow for reactive updates

    @Query("SELECT * FROM budget_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): BudgetGoal?

    @Update
    suspend fun updateGoal(budgetGoal: BudgetGoal)

    @Delete
    suspend fun deleteGoal(budgetGoal: BudgetGoal)

    @Query("DELETE FROM budget_goals")
    suspend fun deleteAllGoals()

    // You might want to add queries to update currentAmount for a specific goal
    @Query("UPDATE budget_goals SET currentAmount = :newAmount WHERE id = :goalId")
    suspend fun updateCurrentAmount(goalId: Long, newAmount: Double)
}
