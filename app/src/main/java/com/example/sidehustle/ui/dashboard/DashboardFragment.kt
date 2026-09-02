package com.example.sidehustle.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.ui.common.PlaceholderFragment
import kotlinx.coroutines.launch

class DashboardFragment : PlaceholderFragment() {

    override val titleRes = R.string.dashboard_title
    override val descriptionRes = R.string.dashboard_loading_api

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadApiProfile()
    }

    private fun loadApiProfile() {
        val repository = (requireActivity().application as SideHustleApp).repository

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchMe()) {
                is ApiResult.Success -> {
                    val email = result.data.email ?: getString(R.string.dashboard_api_unknown_email)
                    binding.description.text = getString(R.string.dashboard_api_connected, email)
                }
                is ApiResult.Error -> {
                    binding.description.text = result.message
                }
            }
        }
    }
}
