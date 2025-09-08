package com.billtrack.ui.historylist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billtrack.data.local.model.ExpenseRecord
import com.billtrack.databinding.ItemHistoryEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryListAdapter(
    private val onItemClicked: (ExpenseRecord) -> Unit
) : ListAdapter<ExpenseRecord, HistoryListAdapter.HistoryViewHolder>(ExpenseRecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record, onItemClicked)
    }

    class HistoryViewHolder(private val binding: ItemHistoryEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(record: ExpenseRecord, onItemClicked: (ExpenseRecord) -> Unit) {
            binding.historyItemAmountTextView.text = record.selectedAmountText 
            binding.historyItemDateTextView.text = dateFormatter.format(Date(record.timestamp))
            binding.root.setOnClickListener {
                onItemClicked(record)
            }
        }
    }

    class ExpenseRecordDiffCallback : DiffUtil.ItemCallback<ExpenseRecord>() {
        override fun areItemsTheSame(oldItem: ExpenseRecord, newItem: ExpenseRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpenseRecord, newItem: ExpenseRecord): Boolean {
            return oldItem == newItem
        }
    }
}
