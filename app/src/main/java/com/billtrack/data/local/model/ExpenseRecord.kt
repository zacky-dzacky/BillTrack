package com.billtrack.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) 
    var id: Int = 0,

    @ColumnInfo(name = "selected_amount_text") 
    val selectedAmountText: String,

    @ColumnInfo(name = "image_file_path") 
    val imageFilePath: String,

    @ColumnInfo(name = "timestamp") 
    val timestamp: Long
)
