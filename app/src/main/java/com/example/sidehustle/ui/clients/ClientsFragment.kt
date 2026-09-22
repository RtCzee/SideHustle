package com.example.sidehustle.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.databinding.FragmentClientsBinding
import com.example.sidehustle.ui.common.loading.LoadingStateHandler
import com.example.sidehustle.ui.common.loading.applySideHustleStyle
import com.example.sidehustle.ui.common.loading.forViews
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Clients list. For now this shows DEMO data (see [fetchClientsDemo]) so the shared loading
 * patterns can be seen working on a real list screen:
 *
 *  - first load  -> list skeleton (shimmer)
 *  - pull down   -> pull-to-refresh indicator, existing rows stay visible
 *
 * TODO(#14): replace [fetchClientsDemo] with the repository call and the real client model.
 * The loading wiring below should not need to change.
 */
class ClientsFragment : Fragment() {

    private var _binding: FragmentClientsBinding? = null
    private val binding get() = _binding!!

    private var loading: LoadingStateHandler? = null

    private val adapter = ClientRowAdapter {
        findNavController().navigate(R.id.action_clientsFragment_to_clientDetailsFragment)
    }

    // Each load gets a number; only the newest load is allowed to clear the indicators.
    private var loadGeneration = 0
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clientsList.adapter = adapter

        loading = LoadingStateHandler.forViews(
            loadingView = binding.listLoading.root,
            contentView = binding.swipeRefresh,
            lifecycleOwner = viewLifecycleOwner,
        )

        binding.swipeRefresh.applySideHustleStyle()
        binding.swipeRefresh.setOnRefreshListener { loadClients(fromPullToRefresh = true) }

        if (adapter.itemCount == 0) loadClients(fromPullToRefresh = false)
    }

    private fun loadClients(fromPullToRefresh: Boolean) {
        val generation = ++loadGeneration
        loadJob?.cancel() // a newer request supersedes any load still in flight
        // First load has nothing to show yet, so use the skeleton. A pull-to-refresh already
        // shows its own indicator over the existing rows.
        if (!fromPullToRefresh) loading?.setLoading(true)

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                adapter.submitList(fetchClientsDemo())
            } finally {
                // Runs on success, failure and cancellation. A newer load owns the indicators.
                // The view may already be gone (late response), so never touch `binding` here.
                if (generation == loadGeneration) {
                    loading?.setLoading(false)
                    _binding?.swipeRefresh?.isRefreshing = false
                }
            }
        }
    }

    /** Simulates a network call. TODO(#14): remove. */
    private suspend fun fetchClientsDemo(): List<DemoClient> {
        delay(DEMO_NETWORK_DELAY_MS)
        val updatedAt = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        return DEMO_NAMES.mapIndexed { index, name ->
            DemoClient(id = index.toLong(), name = name, updatedAt = updatedAt)
        }
    }

    override fun onDestroyView() {
        loading = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DEMO_NETWORK_DELAY_MS = 1500L
        val DEMO_NAMES = listOf(
            "Thabo Mokoena", "Lerato Dlamini", "Pieter van der Merwe", "Ayesha Patel",
            "Sipho Nkosi", "Karen Botha", "Naledi Khumalo", "Johan Pretorius",
            "Zanele Mahlangu", "Ruan Steyn", "Fatima Essop", "Bongani Zulu",
        )
    }
}
