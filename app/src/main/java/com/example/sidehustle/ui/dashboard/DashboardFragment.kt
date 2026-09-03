package com.example.sidehustle.ui.dashboard

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Home screen — loads metrics from GET /dashboard, falls back to GET /me if needed.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showPlaceholders()
        loadDashboard()
    }

    private fun showPlaceholders() {
        binding.welcomeName.text = FirebaseAuth.getInstance().currentUser?.displayName
            ?: getString(R.string.dashboard_name_placeholder)
        binding.scoreValue.text = getString(R.string.dashboard_zero)
        binding.jobsValue.text = getString(R.string.dashboard_zero)
        binding.metricIncomeValue.text = getString(R.string.dashboard_money_placeholder)
        binding.metricExpensesValue.text = getString(R.string.dashboard_money_placeholder)
        binding.metricProfitValue.text = getString(R.string.dashboard_money_placeholder)
        binding.metricOutstandingValue.text = getString(R.string.dashboard_money_placeholder)
    }

    private fun loadDashboard() {
        val repository = (requireActivity().application as SideHustleApp).repository
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchDashboard()) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Dashboard loaded for ${result.data.fullName}")
                    binding.statusMessage.isVisible = false
                    showData(result.data)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Dashboard API failed: ${result.message} (HTTP ${result.httpCode})")
                    loadProfileFallback(result.message)
                }
            }
            setLoading(false)
        }
    }

    /** If /dashboard is missing or fails, still show name from GET /me. */
    private suspend fun loadProfileFallback(dashboardError: String) {
        val repository = (requireActivity().application as SideHustleApp).repository
        when (val profileResult = repository.fetchProfile()) {
            is ApiResult.Success -> {
                Log.d(TAG, "Profile fallback loaded for ${profileResult.data.fullName}")
                showProfileOnly(profileResult.data)
                binding.statusMessage.isVisible = true
                binding.statusMessage.text = getString(R.string.dashboard_metrics_unavailable)
            }
            is ApiResult.Error -> {
                Log.e(TAG, "Profile fallback failed: ${profileResult.message}")
                binding.statusMessage.isVisible = true
                binding.statusMessage.text = dashboardError
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingIndicator.isVisible = loading
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
        binding.scoreValue.text = getString(R.string.dashboard_zero)
        binding.jobsValue.text = getString(R.string.dashboard_zero)
        val zero = formatMoney(profile.preferredCurrency, 0.0)
        binding.metricIncomeValue.text = zero
        binding.metricExpensesValue.text = zero
        binding.metricProfitValue.text = zero
        binding.metricOutstandingValue.text = zero
    }

    private fun formatMoney(currency: String, amount: Double): String {
        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
        return if (currency == "ZAR") "R $formatted" else "$currency $formatted"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
