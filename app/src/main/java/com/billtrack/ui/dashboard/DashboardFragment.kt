package com.billtrack.ui.dashboard
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.billtrack.databinding.FragmentDashboardBinding
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
     private val binding get() = _binding!!
      override fun onCreateView(
     inflater: LayoutInflater,
     container: ViewGroup?,
     savedInstanceState: Bundle?): View {
          _binding = FragmentDashboardBinding.inflate(inflater, container, false)
          val root: View = binding.root
            // TODO: Initialize UI elements and set up listeners if needed
            // For example:
             binding.incomeValueTextView.text = "$0.00"
             binding.expensesValueTextView.text = "Rp 100.000.0"
            return root
      }
    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
    }

}