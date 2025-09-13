package com.billtrack.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    var currentAmount: Double = 0.0, // Default to 0, can be updated later
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)
