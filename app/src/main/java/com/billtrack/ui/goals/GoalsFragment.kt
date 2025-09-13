package com.billtrack.ui.goals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.billtrack.R
import com.billtrack.databinding.FragmentGoalsBinding
import com.billtrack.databinding.ItemBudgetGoalBinding

// Data class for budget goal items
data class BudgetGoalItem(
    val name: String,
    val progressText: String,
    val percentageText: String,
    val progressValue: Int,
    val progressColorRes: Int // Color resource ID for the progress bar
)

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var budgetGoalsAdapter: BudgetGoalsAdapter
    private val currentGoalsList = mutableListOf<BudgetGoalItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.goals_fragment_title)

        setupCreateGoalSection()
        setupCurrentGoalsRecyclerView()
        loadSampleGoals()
    }

    private fun setupCreateGoalSection() {
        // Setup category spinner
        val categories = arrayOf("Groceries", "Entertainment", "Transportation", "Savings", "Utilities", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.goalCategoryAutoCompleteTextView.setAdapter(adapter)

        binding.setGoalButton.setOnClickListener {
            val goalName = binding.goalNameEditText.text.toString()
            val amount = binding.goalAmountEditText.text.toString()
            val category = binding.goalCategoryAutoCompleteTextView.text.toString()
            if (goalName.isNotBlank() && amount.isNotBlank() && category.isNotBlank()) {
                Toast.makeText(requireContext(), "Goal set: $goalName, $amount, $category", Toast.LENGTH_LONG).show()
                // TODO: Add logic to save the goal and update the current goals list
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCurrentGoalsRecyclerView() {
        budgetGoalsAdapter = BudgetGoalsAdapter(currentGoalsList, requireContext())
        binding.currentGoalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = budgetGoalsAdapter
        }
    }

    private fun loadSampleGoals() {
        currentGoalsList.clear()
        currentGoalsList.add(
            BudgetGoalItem(
                name = getString(R.string.goals_example_groceries),
                progressText = getString(R.string.goals_example_groceries_progress),
                percentageText = getString(R.string.goals_example_groceries_percentage),
                progressValue = 50,
                progressColorRes = R.color.goal_progress_groceries
            )
        )
        currentGoalsList.add(
            BudgetGoalItem(
                name = getString(R.string.goals_example_entertainment),
                progressText = getString(R.string.goals_example_entertainment_progress),
                percentageText = getString(R.string.goals_example_entertainment_percentage),
                progressValue = 60,
                progressColorRes = R.color.goal_progress_entertainment
            )
        )
        currentGoalsList.add(
            BudgetGoalItem(
                name = getString(R.string.goals_example_transportation),
                progressText = getString(R.string.goals_example_transportation_progress),
                percentageText = getString(R.string.goals_example_transportation_percentage),
                progressValue = 95,
                progressColorRes = R.color.goal_progress_transportation
            )
        )
        budgetGoalsAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter for Current Goals RecyclerView
class BudgetGoalsAdapter(
    private val goalsList: List<BudgetGoalItem>,
    private val context: Context
) : RecyclerView.Adapter<BudgetGoalsAdapter.BudgetGoalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val binding = ItemBudgetGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        holder.bind(goalsList[position], context)
    }

    override fun getItemCount(): Int = goalsList.size

    class BudgetGoalViewHolder(private val binding: ItemBudgetGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goalItem: BudgetGoalItem, context: Context) {
            binding.goalNameTextView.text = goalItem.name
            binding.goalProgressTextView.text = goalItem.progressText
            binding.goalPercentageTextView.text = goalItem.percentageText
            binding.goalProgressBar.progress = goalItem.progressValue
            binding.goalProgressBar.progressTintList = ContextCompat.getColorStateList(context, goalItem.progressColorRes)
            
            // Set percentage text color based on progress color (optional, for better visual consistency)
            // For simplicity, using the direct color. Could also create specific text color resources.
            binding.goalPercentageTextView.setTextColor(ContextCompat.getColor(context, goalItem.progressColorRes))
        }
    }
}
