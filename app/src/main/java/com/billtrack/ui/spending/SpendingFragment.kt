package com.billtrack.ui.spending

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.billtrack.BillCaptureActivity
import com.billtrack.R
import com.billtrack.databinding.FragmentSpendingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpendingFragment : Fragment() {

    private var _binding: FragmentSpendingBinding? = null
    private val binding get() = _binding!!

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.log_expense_fragment_title)

        setupDateField()
        setupCategorySpinner()
        setupClickListeners()
    }

    private fun setupDateField() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        binding.dateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        // Also allow clicking the end icon in TextInputLayout if needed (though EditText click is usually enough)
        binding.dateInputLayout.setEndIconOnClickListener {
             DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateInView() {
        val myFormat = "MM/dd/yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.dateEditText.setText(sdf.format(calendar.time))
    }

    private fun setupCategorySpinner() {
        // Replace with your actual categories
        val categories = arrayOf("Food", "Transport", "Entertainment", "Utilities", "Shopping", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.addNewCategoryButton.setOnClickListener {
            Toast.makeText(requireContext(), "Add New Category clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement add new category functionality
        }

        binding.captureBillImageButton.setOnClickListener {
            startActivity(Intent(requireContext(), BillCaptureActivity::class.java));
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
