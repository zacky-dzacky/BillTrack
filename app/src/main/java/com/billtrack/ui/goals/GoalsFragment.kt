package com.billtrack.ui.goals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.billtrack.R
import com.billtrack.data.local.AppDatabase
import com.billtrack.data.local.dao.BudgetGoalDao
import com.billtrack.data.local.model.BudgetGoal
import com.billtrack.databinding.FragmentGoalsBinding
import com.billtrack.databinding.ItemBudgetGoalBinding
import com.billtrack.utils.BillTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

// Data class for budget goal items (UI model)
data class BudgetGoalItem(
    val id: Long, // Keep original DB id for potential future operations (delete/update)
    val name: String,
    val progressText: String,
    val percentageText: String,
    val progressValue: Int,
    val progressColorRes: Int
)

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var budgetGoalDao: BudgetGoalDao
    private lateinit var budgetGoalsAdapter: BudgetGoalsAdapter

    // Define categories for spinner and color mapping
    private val goalCategories = arrayOf("Groceries", "Entertainment", "Transportation", "Savings", "Utilities", "Other")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        budgetGoalDao = AppDatabase.getDatabase(requireContext()).budgetGoalDao()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.goals_fragment_title)

        setupCreateGoalSection()
        setupCurrentGoalsRecyclerView()
        observeAndDisplayGoals()
    }

    private fun setupCreateGoalSection() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, goalCategories)
        binding.goalCategoryAutoCompleteTextView.setAdapter(adapter)

        binding.setGoalButton.setOnClickListener {
            val goalName = binding.goalNameEditText.text.toString().trim()
            val amountStr = binding.goalAmountEditText.text.toString().trim()
            val category = binding.goalCategoryAutoCompleteTextView.text.toString().trim()

            if (goalName.isBlank() || amountStr.isBlank() || category.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetAmount: Double
            try {
                targetAmount = amountStr.toDouble()
                if (targetAmount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be positive", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Invalid amount entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newGoal = BudgetGoal(
                name = goalName,
                targetAmount = targetAmount,
                currentAmount = 0.0, // New goals start with 0 progress
                category = category
            )

            lifecycleScope.launch(Dispatchers.IO) {
                budgetGoalDao.insertGoal(newGoal)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Goal '$goalName' set!", Toast.LENGTH_SHORT).show()
                    binding.goalNameEditText.text?.clear()
                    binding.goalAmountEditText.text?.clear()
                    binding.goalCategoryAutoCompleteTextView.text?.clear()
                    binding.goalNameEditText.requestFocus() // Optional: move focus back
                }
            }
        }
    }

    private fun setupCurrentGoalsRecyclerView() {
        budgetGoalsAdapter = BudgetGoalsAdapter(requireContext())
        binding.currentGoalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = budgetGoalsAdapter
        }
    }

    private fun observeAndDisplayGoals() {
        lifecycleScope.launch {
            budgetGoalDao.getAllGoals().collectLatest { dbGoalsList ->
                val uiGoalsList = dbGoalsList.map { dbGoal -> transformBudgetGoalToItem(dbGoal) }
                budgetGoalsAdapter.submitList(uiGoalsList)
            }
        }
    }

    private fun transformBudgetGoalToItem(dbGoal: BudgetGoal): BudgetGoalItem {
        val percentage = if (dbGoal.targetAmount > 0) (dbGoal.currentAmount / dbGoal.targetAmount * 100) else 0.0
        val progressValue = percentage.toInt().coerceIn(0, 100)
        val currencyFormat = BillTextParser

        return BudgetGoalItem(
            id = dbGoal.id,
            name = dbGoal.name,
            progressText = "${currencyFormat.formatNumberWithSeparator(dbGoal.currentAmount)} / ${currencyFormat.formatNumberWithSeparator(dbGoal.targetAmount)}",
            percentageText = "${progressValue}%",
            progressValue = progressValue,
            progressColorRes = getProgressColorForCategory(dbGoal.category)
        )
    }

    private fun getProgressColorForCategory(category: String): Int {
        return when (category.lowercase(Locale.getDefault())) {
            "groceries" -> R.color.goal_progress_groceries
            "entertainment" -> R.color.goal_progress_entertainment
            "transportation" -> R.color.goal_progress_transportation
            "savings" -> R.color.dashboard_progress_bar_color // Example for new categories
            "utilities" -> R.color.teal_700 // Example for new categories
            else -> R.color.color_primary // Default color for 'Other' or undefined
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// DiffUtil Callback for BudgetGoalItem
class BudgetGoalDiffCallback : DiffUtil.ItemCallback<BudgetGoalItem>() {
    override fun areItemsTheSame(oldItem: BudgetGoalItem, newItem: BudgetGoalItem): Boolean {
        return oldItem.id == newItem.id // Use unique ID from database
    }

    override fun areContentsTheSame(oldItem: BudgetGoalItem, newItem: BudgetGoalItem): Boolean {
        return oldItem == newItem
    }
}

// Updated Adapter for Current Goals RecyclerView using ListAdapter
class BudgetGoalsAdapter(
    private val context: Context
) : ListAdapter<BudgetGoalItem, BudgetGoalsAdapter.BudgetGoalViewHolder>(BudgetGoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val binding = ItemBudgetGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        holder.bind(getItem(position), context)
    }

    class BudgetGoalViewHolder(private val binding: ItemBudgetGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goalItem: BudgetGoalItem, context: Context) {
            binding.goalNameTextView.text = goalItem.name
            binding.goalProgressTextView.text = goalItem.progressText
            binding.goalPercentageTextView.text = goalItem.percentageText
            binding.goalProgressBar.progress = goalItem.progressValue

            // Apply tint to progress bar and text color for percentage
            val progressColor = ContextCompat.getColor(context, goalItem.progressColorRes)
            binding.goalProgressBar.progressTintList = ContextCompat.getColorStateList(context, goalItem.progressColorRes)
            binding.goalPercentageTextView.setTextColor(progressColor)
        }
    }
}
