package com.example.sidehustle.ui.projects

import android.os.Bundle
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
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showFullPageError
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.loading.applySideHustleStyle
import com.example.sidehustle.ui.navigation.navigateSafely
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** First-load errors replace the list; refresh errors retain rows and offer Retry inline. */
class ProjectsFragment : Fragment() {
    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JobsAdapter
    private var loadJob: Job? = null
    private var hasLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = JobsAdapter { job -> openDetails(job.jobId) }
        binding.jobsList.layoutManager = LinearLayoutManager(requireContext())
        binding.jobsList.adapter = adapter
        binding.addButton.setOnClickListener { openDetails(null) }
        binding.emptyAddButton.setOnClickListener { openDetails(null) }
        binding.swipeRefresh.applySideHustleStyle()
        binding.swipeRefresh.setOnRefreshListener { loadJobs() }
    }

    override fun onResume() {
        super.onResume()
        loadJobs()
    }

    private fun openDetails(jobId: String?) {
        findNavController().navigateSafely(
            R.id.action_projectsFragment_to_projectDetailsFragment,
            bundleOf(ProjectDetailsFragment.ARG_JOB_ID to jobId,
                ProjectDetailsFragment.ARG_IS_CREATING to (jobId == null)),
        )
    }

    private fun loadJobs() {
        val requestBinding = _binding ?: return
        loadJob?.cancel()
        dismissInlineError()
        binding.errorState.isVisible = false
        binding.loadingIndicator.isVisible = !hasLoaded
        if (!hasLoaded) {
            binding.emptyState.isVisible = false
            binding.swipeRefresh.isVisible = false
        } else {
            binding.swipeRefresh.isVisible = adapter.itemCount > 0
            binding.emptyState.isVisible = adapter.itemCount == 0
            binding.swipeRefresh.isRefreshing = true
        }
        val repository = (requireActivity().application as SideHustleApp).repository
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = repository.fetchJobs()
                coroutineContext.ensureActive()
                if (_binding !== requestBinding) return@launch
                when (result) {
                    is ApiResult.Success -> showJobs(result.data)
                    is ApiResult.Error -> {
                        if (!hasLoaded || result.httpCode == 401) {
                            binding.swipeRefresh.isVisible = false
                            binding.emptyState.isVisible = false
                            showFullPageError(binding.errorState, result, ::loadJobs)
                        } else {
                            showInlineError(result, ::loadJobs)
                        }
                    }
                }
            } finally {
                if (isActive && _binding === requestBinding) {
                    requestBinding.loadingIndicator.isVisible = false
                    requestBinding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun showJobs(jobs: List<JobResponse>) {
        hasLoaded = true
        binding.errorState.isVisible = false
        adapter.submitList(jobs)
        binding.emptyState.isVisible = jobs.isEmpty()
        binding.swipeRefresh.isVisible = jobs.isNotEmpty()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        hasLoaded = false
        _binding = null
        super.onDestroyView()
    }
}
