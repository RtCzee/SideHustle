package com.example.sidehustle.ui.projects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.ui.navigation.navigateSafely
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.model.JobRequest
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentJobDetailsBinding
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showFullPageError
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.navigation.isValidDetailRequest
import com.example.sidehustle.util.AuthValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Explicit isCreating opens the add form. Otherwise a valid jobId is required;
 * missing or malformed IDs show the shared 404 state.
 */
class ProjectDetailsFragment : Fragment() {

    private var _binding: FragmentJobDetailsBinding? = null
    private val binding get() = _binding!!

    private val jobId: String? get() = arguments?.get(ARG_JOB_ID) as? String
    private val isEditMode: Boolean get() = jobId != null

    private var clients: List<ClientResponse> = emptyList()
    private var selectedClientId: String? = null
    private val isCreating: Boolean get() = arguments?.get(ARG_IS_CREATING) == true
    private var loadJob: Job? = null

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
            when {
                !isValidDetailRequest(jobId, isCreating) -> R.string.project_details_title
                isEditMode -> R.string.job_details_edit_title
                else -> R.string.job_details_add_title
            }
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

        binding.needsClientAddButton.setOnClickListener {
            findNavController().navigateSafely(R.id.clientDetailsFragment, bundleOf("isCreating" to true))
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.formGroup.isVisible) refreshClientsOnly() else load()
    }

    private fun load() {
        loadJob?.cancel()
        dismissInlineError()
        binding.errorState.isVisible = false
        binding.needsClientGroup.isVisible = false
        binding.formGroup.isVisible = false
        if (!isValidDetailRequest(jobId, isCreating)) {
            showLoadError(ApiResult.Error("", 404))
            return
        }
        binding.loadingIndicator.isVisible = true
        val repository = (requireActivity().application as SideHustleApp).repository
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Resolve the resource first, so a missing job always gets a 404 state.
                if (isEditMode) {
                    when (val result = repository.fetchJob(jobId!!)) {
                        is ApiResult.Success -> populateFields(result.data)
                        is ApiResult.Error -> {
                            showLoadError(result)
                            return@launch
                        }
                    }
                }
                when (val result = repository.fetchClients()) {
                    is ApiResult.Success -> {
                        clients = result.data
                        bindClientAdapter()
                        if (clients.isEmpty() && !isEditMode) {
                            binding.needsClientGroup.isVisible = true
                        } else {
                            if (!isEditMode) applyCreateDefaults()
                            binding.formGroup.isVisible = true
                        }
                    }
                    is ApiResult.Error -> showLoadError(result)
                }
            } finally {
                if (isActive) _binding?.loadingIndicator?.isVisible = false
            }
        }
    }

    private fun showLoadError(error: ApiResult.Error) {
        binding.loadingIndicator.isVisible = false
        binding.formGroup.isVisible = false
        showFullPageError(binding.errorState, error, ::load)
    }

    private fun refreshClientsOnly() {
        loadJob?.cancel()
        dismissInlineError()
        val repository = (requireActivity().application as SideHustleApp).repository
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchClients()) {
                is ApiResult.Success -> {
                    clients = result.data
                    bindClientAdapter()
                }
                is ApiResult.Error -> showInlineError(result, ::refreshClientsOnly)
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
        dismissInlineError()
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
                    showInlineError(result, ::submitSave)
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
        loadJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
        const val ARG_JOB_ID = "jobId"
        const val ARG_IS_CREATING = "isCreating"
    }
}
