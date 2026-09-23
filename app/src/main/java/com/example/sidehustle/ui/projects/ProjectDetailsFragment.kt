package com.example.sidehustle.ui.projects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.JobRequest
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentJobDetailsBinding
import com.example.sidehustle.util.AuthValidator
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Add/edit a single job (issue: Build Job Management). With no jobId argument this is
 * "add job" (blank form, POST on save, must pick an existing client). With a jobId it
 * loads GET /jobs/{id}, allows editing every field including status (PUT).
 */
class ProjectDetailsFragment : Fragment() {

    private var _binding: FragmentJobDetailsBinding? = null
    private val binding get() = _binding!!

    private val jobId: String? by lazy { arguments?.getString(ARG_JOB_ID) }
    private val isEditMode: Boolean get() = jobId != null

    private var clients: List<ClientResponse> = emptyList()
    private var selectedClientId: String? = null
    private var hasLoadedOnce = false

    private val statuses: List<String> by lazy { resources.getStringArray(R.array.job_statuses).toList() }
    private val isoDateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title.setText(
            if (isEditMode) R.string.job_details_edit_title else R.string.job_details_add_title
        )

        binding.statusInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statuses)
        )
        binding.statusInput.setOnItemClickListener { _, _, position, _ ->
            // Nice-to-have: auto-fill today's date the moment a job is marked Completed,
            // if the user hasn't already set one. Still fully editable/clearable after.
            val status = statuses.getOrNull(position)
            if (status == "Completed" && binding.completedDateInput.text.isNullOrBlank()) {
                binding.completedDateInput.setText(isoDateFormat.format(Date()))
            }
        }

        binding.startDateInput.setOnClickListener {
            showDatePicker(binding.startDateInput) { binding.startDateInput.setText(it) }
        }
        binding.dueDateInput.setOnClickListener {
            showDatePicker(binding.dueDateInput) { binding.dueDateInput.setText(it) }
        }
        binding.completedDateInput.setOnClickListener {
            showDatePicker(binding.completedDateInput) { binding.completedDateInput.setText(it) }
        }

        binding.saveButton.setOnClickListener { submitSave() }
        binding.retryButton.setOnClickListener { load() }
        binding.needsClientAddButton.setOnClickListener {
            findNavController().navigate(R.id.clientDetailsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasLoadedOnce) {
            hasLoadedOnce = true
            load()
        } else {
            // Covers returning from "add a client" — refresh the picker without
            // disturbing whatever the user has already typed into the form.
            refreshClientsOnly()
        }
    }

    private fun load() {
        val repository = (requireActivity().application as SideHustleApp).repository

        binding.errorGroup.isVisible = false
        binding.needsClientGroup.isVisible = false
        binding.formGroup.isVisible = false
        binding.loadingIndicator.isVisible = true

        viewLifecycleOwner.lifecycleScope.launch {
            when (val clientsResult = repository.fetchClients()) {
                is ApiResult.Success -> {
                    clients = clientsResult.data
                    bindClientAdapter()

                    if (clients.isEmpty() && !isEditMode) {
                        binding.loadingIndicator.isVisible = false
                        binding.needsClientGroup.isVisible = true
                        return@launch
                    }

                    if (isEditMode) {
                        loadJob()
                    } else {
                        binding.loadingIndicator.isVisible = false
                        applyCreateDefaults()
                        binding.formGroup.isVisible = true
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load clients: ${clientsResult.message}")
                    binding.loadingIndicator.isVisible = false
                    binding.errorMessage.text = clientsResult.message.ifBlank {
                        getString(R.string.clients_load_error)
                    }
                    binding.errorGroup.isVisible = true
                }
            }
        }
    }

    private suspend fun loadJob() {
        val id = jobId ?: return
        val repository = (requireActivity().application as SideHustleApp).repository

        when (val result = repository.fetchJob(id)) {
            is ApiResult.Success -> {
                Log.d(TAG, "Loaded job $id")
                populateFields(result.data)
                binding.loadingIndicator.isVisible = false
                binding.formGroup.isVisible = true
            }
            is ApiResult.Error -> {
                Log.e(TAG, "Failed to load job $id: ${result.message}")
                binding.loadingIndicator.isVisible = false
                binding.errorMessage.text = result.message.ifBlank {
                    getString(R.string.job_load_error)
                }
                binding.errorGroup.isVisible = true
            }
        }
    }

    private fun refreshClientsOnly() {
        val repository = (requireActivity().application as SideHustleApp).repository
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchClients()) {
                is ApiResult.Success -> {
                    clients = result.data
                    bindClientAdapter()
                    if (clients.isNotEmpty() && binding.needsClientGroup.isVisible) {
                        binding.needsClientGroup.isVisible = false
                        applyCreateDefaults()
                        binding.formGroup.isVisible = true
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Background client refresh failed: ${result.message}")
                    // Keep whatever was already on screen — this is a best-effort refresh.
                }
            }
        }
    }

    private fun bindClientAdapter() {
        binding.clientInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, clients.map { it.name })
        )
        binding.clientInput.setOnItemClickListener { _, _, position, _ ->
            selectedClientId = clients.getOrNull(position)?.clientId
            binding.clientLayout.error = null
        }
    }

    private fun applyCreateDefaults() {
        if (binding.statusInput.text.isNullOrBlank()) {
            binding.statusInput.setText(statuses.first(), false)
        }
        if (binding.amountInput.text.isNullOrBlank()) {
            binding.amountInput.setText("0")
        }
    }

    private fun populateFields(job: JobResponse) {
        selectedClientId = job.clientId
        binding.clientInput.setText(job.clientName, false)
        binding.titleInput.setText(job.title)
        binding.descriptionInput.setText(job.description.orEmpty())
        binding.statusInput.setText(job.status, false)
        binding.amountLayout.hint = getString(R.string.hint_agreed_amount_with_currency, job.currency)
        binding.amountInput.setText(formatAmountForInput(job.agreedAmount))
        binding.startDateInput.setText(job.startDate.orEmpty())
        binding.dueDateInput.setText(job.dueDate.orEmpty())
        binding.completedDateInput.setText(job.completedDate.orEmpty())
        clearFieldErrors()
    }

    private fun clearFieldErrors() {
        binding.clientLayout.error = null
        binding.titleLayout.error = null
        binding.amountLayout.error = null
    }

    private fun formatAmountForInput(amount: Double): String =
        if (amount == Math.floor(amount) && !amount.isInfinite()) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }

    private fun showDatePicker(target: android.widget.EditText, onPicked: (String) -> Unit) {
        val current = target.text?.toString().orEmpty()
        val selection = current.takeIf { it.isNotBlank() }
            ?.let { runCatching { isoDateFormat.parse(it)?.time }.getOrNull() }
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selection)
            .build()
        picker.addOnPositiveButtonClickListener { millis -> onPicked(isoDateFormat.format(Date(millis))) }
        picker.show(parentFragmentManager, "job_date_picker")
    }

    private fun submitSave() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val description = binding.descriptionInput.text?.toString()?.trim().orEmpty()
        val status = binding.statusInput.text?.toString()?.trim()?.ifBlank { null } ?: statuses.first()
        val amountText = binding.amountInput.text?.toString()?.trim().orEmpty()
        val startDate = binding.startDateInput.text?.toString()?.trim().orEmpty()
        val dueDate = binding.dueDateInput.text?.toString()?.trim().orEmpty()
        val completedDate = binding.completedDateInput.text?.toString()?.trim().orEmpty()

        binding.titleLayout.error = AuthValidator.titleError(title)?.let { getString(it) }
        binding.amountLayout.error = AuthValidator.amountError(amountText)?.let { getString(it) }
        binding.clientLayout.error = if (selectedClientId == null) {
            getString(R.string.error_client_required)
        } else {
            null
        }

        if (binding.titleLayout.error != null ||
            binding.amountLayout.error != null ||
            binding.clientLayout.error != null
        ) {
            return
        }

        val request = JobRequest(
            clientId = selectedClientId!!,
            title = title,
            description = description,
            status = status,
            startDate = startDate,
            dueDate = dueDate,
            completedDate = completedDate,
            agreedAmount = amountText.toDouble(),
        )

        val repository = (requireActivity().application as SideHustleApp).repository
        setSaving(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = if (isEditMode) {
                repository.updateJob(jobId!!, request)
            } else {
                repository.createJob(request)
            }

            when (result) {
                is ApiResult.Success -> {
                    setSaving(false)
                    if (isEditMode) {
                        Log.d(TAG, "Updated job ${result.data.jobId}")
                        populateFields(result.data)
                        Snackbar.make(binding.root, R.string.job_saved_message, Snackbar.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "Created job ${result.data.jobId}")
                        Snackbar.make(binding.root, R.string.job_added_message, Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to save job: ${result.message}")
                    setSaving(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        binding.saveButton.isEnabled = !saving
        binding.saveButton.text = getString(
            if (saving) R.string.action_save_loading else R.string.action_save
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
        const val ARG_JOB_ID = "jobId"
    }
}
