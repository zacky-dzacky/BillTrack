package com.billtrack.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.billtrack.R
import com.billtrack.databinding.FragmentReportBinding
import com.google.android.material.button.MaterialButton

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.report_fragment_title)

        setupToggleButtons()
        setupFilterButtons()
    }

    private fun setupToggleButtons() {
        // Set initial selected state for the default button
//        updateToggleButtonAppearance(binding.categoriesToggleButton, true)
//        updateToggleButtonAppearance(binding.timeToggleButton, false)
//        updateToggleButtonAppearance(binding.merchantsToggleButton, false)

//        binding.reportTypeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
//            // First, set all to unselected appearance
//            updateToggleButtonAppearance(binding.categoriesToggleButton, false)
//            updateToggleButtonAppearance(binding.timeToggleButton, false)
//            updateToggleButtonAppearance(binding.merchantsToggleButton, false)
//
//            // Then, set the currently checked one to selected appearance
//            val checkedButton = group.findViewById<MaterialButton>(checkedId)
//            if (checkedButton != null && isChecked) {
//                updateToggleButtonAppearance(checkedButton, true)
//                // Handle logic for tab change if needed
//                // For example, load different report data based on the selected toggle
//                when (checkedId) {
//                    R.id.categoriesToggleButton -> {
//                        // Load categories report
//                        Toast.makeText(requireContext(), "Categories view selected", Toast.LENGTH_SHORT).show()
//                    }
//                    R.id.timeToggleButton -> {
//                        // Load time-based report
//                        Toast.makeText(requireContext(), "Time view selected", Toast.LENGTH_SHORT).show()
//                    }
//                    R.id.merchantsToggleButton -> {
//                        // Load merchants report
//                        Toast.makeText(requireContext(), "Merchants view selected", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
    }

    private fun updateToggleButtonAppearance(button: MaterialButton, selected: Boolean) {
        button.isSelected = selected // This helps with the text color selector
        if (selected) {
            button.background = ContextCompat.getDrawable(requireContext(), R.drawable.report_toggle_button_selected_bg)
        } else {
            button.background = ContextCompat.getDrawable(requireContext(), R.drawable.report_toggle_button_unselected_bg)
        }
    }

    private fun setupFilterButtons() {
        binding.categoryFilterButton.setOnClickListener {
            // TODO: Implement category filter logic (e.g., show a dropdown or dialog)
            Toast.makeText(requireContext(), "Category filter clicked", Toast.LENGTH_SHORT).show()
        }

        binding.dateRangeFilterButton.setOnClickListener {
            // TODO: Implement date range filter logic (e.g., show a date range picker)
            Toast.makeText(requireContext(), "Date Range filter clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
