package com.example.sidehustle.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.DashboardResponse
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentDashboardBinding
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showFullPageError
import com.example.sidehustle.ui.common.error.showInlineError
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Loads metrics from GET /dashboard, falling back to GET /me for first-load partial content. */
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var loadJob: Job? = null
    private var hasContent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.welcomeName.text = FirebaseAuth.getInstance().currentUser?.displayName
            ?: getString(R.string.dashboard_name_placeholder)
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        loadJob?.cancel()
        dismissInlineError()
        binding.errorState.isVisible = false
        binding.dashboardContent.isVisible = true
        binding.loadingIndicator.isVisible = true
        val repository = (requireActivity().application as SideHustleApp).repository
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = repository.fetchDashboard()) {
                    is ApiResult.Success -> {
                        showData(result.data)
                        hasContent = true
                    }
                    is ApiResult.Error -> {
                        if (result.httpCode == 401 || hasContent) showFailure(result)
                        else loadProfileFallback(result)
                    }
                }
            } finally {
                if (isActive) _binding?.loadingIndicator?.isVisible = false
            }
        }
    }

    private suspend fun loadProfileFallback(dashboardError: ApiResult.Error) {
        val repository = (requireActivity().application as SideHustleApp).repository
        when (val result = repository.fetchProfile()) {
            is ApiResult.Success -> {
                showProfileOnly(result.data)
                hasContent = true
                showInlineError(dashboardError, ::loadDashboard)
            }
            is ApiResult.Error -> showFailure(result)
        }
    }

    private fun showFailure(error: ApiResult.Error) {
        if (hasContent && error.httpCode != 401) {
            showInlineError(error, ::loadDashboard)
        } else {
            binding.dashboardContent.isVisible = false
            showFullPageError(binding.errorState, error, ::loadDashboard)
        }
    }

    private fun showData(data: DashboardResponse) {
        binding.welcomeName.text = data.fullName
        binding.scoreValue.text = data.sideHustleScore.toString()
        binding.jobsValue.text = data.completedJobsThisMonth.toString()
        binding.metricIncomeValue.text = formatMoney(data.preferredCurrency, data.totalIncome)
        binding.metricExpensesValue.text = formatMoney(data.preferredCurrency, data.totalExpenses)
        binding.metricProfitValue.text = formatMoney(data.preferredCurrency, data.netProfit)
        binding.metricOutstandingValue.text = formatMoney(data.preferredCurrency, data.outstandingPayments)
    }

    private fun showProfileOnly(profile: UserProfileResponse) {
        binding.welcomeName.text = profile.fullName
        // Unavailable metrics must not look like successfully loaded zero balances.
        binding.scoreValue.text = getString(R.string.dashboard_unavailable)
        binding.jobsValue.text = getString(R.string.dashboard_unavailable)
        binding.metricIncomeValue.text = getString(R.string.dashboard_unavailable)
        binding.metricExpensesValue.text = getString(R.string.dashboard_unavailable)
        binding.metricProfitValue.text = getString(R.string.dashboard_unavailable)
        binding.metricOutstandingValue.text = getString(R.string.dashboard_unavailable)
    }

    private fun formatMoney(currency: String, amount: Double): String {
        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
        return if (currency == "ZAR") "R $formatted" else "$currency $formatted"
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        hasContent = false
        _binding = null
        super.onDestroyView()
    }
}