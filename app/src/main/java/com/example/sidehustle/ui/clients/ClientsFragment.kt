package com.example.sidehustle.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentClientsBinding
import com.example.sidehustle.ui.common.list.ListStateHandler
import com.example.sidehustle.ui.common.loading.applySideHustleStyle
import com.example.sidehustle.ui.navigation.navigateSafely
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClientsFragment : Fragment() {
    private var _binding: FragmentClientsBinding? = null
    private val binding get() = _binding!!
    private var state: ListStateHandler? = null
    private var loadJob: Job? = null
    private lateinit var adapter: ClientsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ClientsAdapter { openDetails(it.clientId) }
        binding.clientsList.adapter = adapter
        state = ListStateHandler(this, binding.listState, binding.swipeRefresh,
            R.string.clients_empty_title, R.string.clients_empty_subtitle, R.string.action_add_client,
            onEmptyAction = { openDetails(null) })
        binding.addButton.setOnClickListener { openDetails(null) }
        binding.swipeRefresh.applySideHustleStyle()
        binding.swipeRefresh.setOnRefreshListener { loadClients() }
    }

    override fun onResume() {
        super.onResume()
        loadClients()
    }

    private fun loadClients() {
        loadJob?.cancel()
        state?.loading()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = (requireActivity().application as SideHustleApp).repository.fetchClients()) {
                    is ApiResult.Success -> {
                        adapter.submitList(result.data)
                        state?.success(result.data.isEmpty())
                    }
                    is ApiResult.Error -> state?.error(result, ::loadClients)
                }
            } finally {
                if (isActive) _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun openDetails(id: String?) {
        findNavController().navigateSafely(R.id.action_clientsFragment_to_clientDetailsFragment,
            bundleOf(ClientDetailsFragment.ARG_CLIENT_ID to id, "isCreating" to (id == null)))
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        state = null
        _binding = null
        super.onDestroyView()
    }
}