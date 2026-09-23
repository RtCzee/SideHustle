package com.example.sidehustle.ui.projects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentJobsBinding
import kotlinx.coroutines.launch

/**
 * Jobs list screen (issue: Build Job Management). Loads GET /jobs, supports
 * pull-to-refresh, an empty state, and navigating to add/edit a job.
 */
class ProjectsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JobsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = JobsAdapter { job -> openDetails(job.jobId) }
        binding.jobsList.layoutManager = LinearLayoutManager(requireContext())
        binding.jobsList.adapter = adapter

        binding.addButton.setOnClickListener { openDetails(jobId = null) }
        binding.emptyAddButton.setOnClickListener { openDetails(jobId = null) }
        binding.retryButton.setOnClickListener { loadJobs(showFullLoading = true) }
        binding.swipeRefresh.setOnRefreshListener { loadJobs(showFullLoading = false) }
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time this screen becomes visible, so returning from add/edit
        // always shows the current list (also covers the very first appearance).
        loadJobs(showFullLoading = true)
    }

    private fun openDetails(jobId: String?) {
        findNavController().navigate(
            R.id.action_projectsFragment_to_projectDetailsFragment,
            bundleOf(ProjectDetailsFragment.ARG_JOB_ID to jobId)
        )
    }

    private fun loadJobs(showFullLoading: Boolean) {
        val repository = (requireActivity().application as SideHustleApp).repository

        if (showFullLoading) {
            binding.errorGroup.isVisible = false
            binding.emptyState.isVisible = false
            binding.swipeRefresh.isVisible = false
            binding.loadingIndicator.isVisible = true
        } else {
            binding.swipeRefresh.isRefreshing = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchJobs()) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Loaded ${result.data.size} job(s)")
                    showJobs(result.data)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load jobs: ${result.message}")
                    binding.loadingIndicator.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                    if (showFullLoading) {
                        binding.swipeRefresh.isVisible = false
                        binding.emptyState.isVisible = false
                        binding.errorMessage.text = result.message.ifBlank {
                            getString(R.string.jobs_load_error)
                        }
                        binding.errorGroup.isVisible = true
                    }
                    // On a pull-to-refresh failure, leave whatever was already on screen in place.
                }
            }
        }
    }

    private fun showJobs(jobs: List<JobResponse>) {
        binding.loadingIndicator.isVisible = false
        binding.swipeRefresh.isRefreshing = false
        binding.errorGroup.isVisible = false
        adapter.submitList(jobs)

        if (jobs.isEmpty()) {
            binding.swipeRefresh.isVisible = false
            binding.emptyState.isVisible = true
        } else {
            binding.emptyState.isVisible = false
            binding.swipeRefresh.isVisible = true
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}

