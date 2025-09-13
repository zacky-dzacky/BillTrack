package com.billtrack.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billtrack.R
import com.billtrack.data.local.AppDatabase // Required for DAO
import com.billtrack.data.local.dao.BudgetGoalDao // Required for DAO
import com.billtrack.data.local.dao.ExpenseInteface
import com.billtrack.data.local.impl.ExpenseDaoImpl
import com.billtrack.data.local.model.BudgetGoal // Required for data transformation
import com.billtrack.databinding.FragmentDashboardBinding
import com.billtrack.ui.goals.BudgetGoalItem // Reusing from GoalsFragment package
import com.billtrack.ui.goals.BudgetGoalsAdapter // Reusing from GoalsFragment package
import com.billtrack.utils.BillTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val expenseDao: ExpenseInteface by lazy { ExpenseDaoImpl(requireContext()) }
    private val budgetGoalDao: BudgetGoalDao by lazy { AppDatabase.getDatabase(requireContext()).budgetGoalDao() }
    private lateinit var budgetGoalsAdapter: BudgetGoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Existing income/expense setup
        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                expenseDao.getAllTotalExpenses()
            }
            binding.expensesValueTextView.text = "Rp $record"
        }
        binding.incomeValueTextView.text = "Rp 0" // Placeholder for income

        // Setup for Budget Goals RecyclerView
        setupBudgetGoalsRecyclerView()
        observeAndDisplayBudgetGoals()
    }

    private fun setupBudgetGoalsRecyclerView() {
        budgetGoalsAdapter = BudgetGoalsAdapter(requireContext()) // Using adapter from ui.goals
        binding.budgetGoalsRecyclerViewDashboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = budgetGoalsAdapter
            // Optional: Prevent nested scrolling issues if the RecyclerView is inside a ScrollView
            // isNestedScrollingEnabled = false
        }
    }

    private fun observeAndDisplayBudgetGoals() {
        lifecycleScope.launch {
            budgetGoalDao.getAllGoals().collectLatest { dbGoalsList ->
                // You might want to limit the number of goals shown on the dashboard, e.g., top 3
                // val limitedDbGoalsList = dbGoalsList.take(3)
                val uiGoalsList = dbGoalsList.map { dbGoal -> transformBudgetGoalToUiItemForDashboard(dbGoal) }
                budgetGoalsAdapter.submitList(uiGoalsList)
            }
        }
    }

    private fun transformBudgetGoalToUiItemForDashboard(dbGoal: BudgetGoal): BudgetGoalItem {
        val percentage = if (dbGoal.targetAmount > 0) (dbGoal.currentAmount / dbGoal.targetAmount * 100) else 0.0
        val progressValue = percentage.toInt().coerceIn(0, 100)
        
        // Using BillTextParser for number formatting, assuming it's accessible
        // If not, use NumberFormat.getCurrencyInstance as in GoalsFragment
        // For consistency, let's use NumberFormat directly here too.
        val currencyFormat = BillTextParser

        return BudgetGoalItem(
            id = dbGoal.id,
            name = dbGoal.name,
            progressText = "${currencyFormat.formatNumberWithSeparator(dbGoal.currentAmount)} / ${currencyFormat.formatNumberWithSeparator(dbGoal.targetAmount)}",
            percentageText = "${progressValue}%",
            progressValue = progressValue,
            progressColorRes = getProgressColorForCategoryDashboard(dbGoal.category)
        )
    }

    private fun getProgressColorForCategoryDashboard(category: String): Int {
        return when (category.lowercase(Locale.getDefault())) {
            "groceries" -> R.color.goal_progress_groceries
            "entertainment" -> R.color.goal_progress_entertainment
            "transportation" -> R.color.goal_progress_transportation
            "savings" -> R.color.dashboard_progress_bar_color 
            "utilities" -> R.color.teal_700 
            else -> R.color.color_primary // Default color for 'Other' or undefined
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
