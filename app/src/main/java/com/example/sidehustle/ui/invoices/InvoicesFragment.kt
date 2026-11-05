package com.example.sidehustle.ui.invoices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.CreateInvoiceRequest
import com.example.sidehustle.data.model.InvoiceClient
import com.example.sidehustle.data.model.InvoiceJob
import com.example.sidehustle.data.model.InvoiceResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.util.LocalizedLabels
import com.example.sidehustle.databinding.FragmentInvoicesBinding
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.util.FormValidator
import com.example.sidehustle.ui.common.list.ListStateHandler
import com.example.sidehustle.ui.common.loading.setLoading
import com.example.sidehustle.ui.navigation.navigateSafely
import com.example.sidehustle.ui.navigation.openFinanceForm
import com.example.sidehustle.ui.navigation.setupFinanceForm
import com.example.sidehustle.ui.navigation.setupFinanceNavigation
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class InvoicesFragment : Fragment() {
    private var _binding: FragmentInvoicesBinding? = null
    private val binding get() = _binding!!
    private var clients = emptyList<InvoiceClient>()
    private var jobs = emptyList<InvoiceJob>()
    private var listState: ListStateHandler? = null
    private var listJob: Job? = null
    private var clientsJob: Job? = null
    private var jobsJob: Job? = null
    private var saving = false
    private val repository get() = (requireActivity().application as SideHustleApp).repository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentInvoicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        setupFinanceNavigation(view, R.id.invoicesFragment)
        setupFinanceForm(view, R.string.action_new_invoice, ::loadClients)
        listState = ListStateHandler(this, binding.listState, binding.invoiceList,
            R.string.invoices_empty_title, R.string.invoices_empty_message, R.string.action_new_invoice,
            ::openFinanceForm, binding.listRefreshing)
        binding.refreshList.setOnClickListener { loadInvoices() }
        binding.invoiceClient.onItemSelectedListener = selectionListener { loadJobsForSelectedClient() }
        binding.invoiceJob.onItemSelectedListener = selectionListener { showSelectedTotal() }
        binding.createInvoice.setOnClickListener { createInvoice() }
        loadInvoices()
    }

    private fun loadClients() {
        if (saving) return
        clientsJob?.cancel()
        jobsJob?.cancel()
        clients = emptyList()
        jobs = emptyList()
        binding.invoiceClient.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_client)))
        binding.invoiceJob.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_job)))
        binding.invoiceMessage.isVisible = false
        binding.optionsState.showLoading()
        binding.invoiceClient.isEnabled = false
        binding.invoiceJob.isEnabled = false
        binding.createInvoice.isEnabled = false
        clientsJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchInvoiceClients()) {
                is ApiResult.Success -> {
                    clients = result.data
                    binding.invoiceClient.isEnabled = clients.isNotEmpty()
                    binding.invoiceClient.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_client)) + clients.map { it.name })
                    if (clients.isEmpty()) binding.optionsState.showEmpty(R.string.invoice_no_clients_title,
                        R.string.invoice_no_clients_message, R.string.action_open_clients) {
                        findNavController().navigateSafely(R.id.clientsFragment)
                    } else binding.optionsState.isVisible = false
                    binding.createInvoice.isEnabled = true
                }
                is ApiResult.Error -> binding.optionsState.showError(this@InvoicesFragment, result, ::loadClients)
            }
        }
    }

    private fun loadJobsForSelectedClient() {
        if (saving) return
        jobsJob?.cancel()
        jobs = emptyList()
        binding.invoiceMessage.isVisible = false
        binding.invoiceJob.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_job)))
        binding.invoiceJob.isEnabled = false
        showSelectedTotal()
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1) ?: run {
            if (clients.isNotEmpty()) binding.optionsState.isVisible = false
            return
        }
        binding.optionsState.showLoading()
        jobsJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchInvoiceJobs(client.id)) {
                is ApiResult.Success -> {
                    jobs = result.data
                    binding.invoiceJob.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_job)) + jobs.map { it.title })
                    binding.invoiceJob.isEnabled = jobs.isNotEmpty()
                    if (jobs.isEmpty()) binding.optionsState.showEmpty(R.string.invoice_no_jobs_title,
                        R.string.invoice_no_jobs_message, R.string.action_open_projects) {
                        findNavController().navigateSafely(R.id.projectsFragment)
                    } else binding.optionsState.isVisible = false
                    showSelectedTotal()
                }
                is ApiResult.Error -> binding.optionsState.showError(this@InvoicesFragment, result, ::loadJobsForSelectedClient)
            }
        }
    }

    private fun showSelectedTotal() {
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        binding.invoiceTotal.text = job?.let { formatMoney(it.currency, it.agreedAmount) }
            ?: getString(R.string.invoice_total_placeholder)
        binding.createInvoice.isEnabled = !saving
    }

    private fun createInvoice() {
        if (saving) return
        dismissInlineError()
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1)
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        val error = FormValidator.invoiceError(client?.id, job?.id, job?.agreedAmount)
        binding.invoiceMessage.isVisible = error != null
        if (error != null) {
            binding.invoiceMessage.setText(error)
            return
        }
        val selectedClient = client ?: return
        val selectedJob = job ?: return
        saving = true
        binding.invoiceClient.isEnabled = false
        binding.invoiceJob.isEnabled = false
        binding.toggleForm.isEnabled = false
        binding.createInvoice.setLoading(true, R.string.action_saving)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = repository.createInvoice(CreateInvoiceRequest(selectedClient.id, selectedJob.id))) {
                    is ApiResult.Success -> {
                        binding.entryForm.isVisible = false
                        binding.toggleForm.setText(R.string.action_new_invoice)
                        Snackbar.make(binding.root, R.string.invoice_created, Snackbar.LENGTH_SHORT).show()
                        loadInvoices()
                    }
                    is ApiResult.Error -> showInlineError(result, ::createInvoice)
                }
            } finally {
                if (isActive && _binding != null) {
                    saving = false
                    binding.createInvoice.setLoading(false, R.string.action_saving)
                    binding.invoiceClient.isEnabled = clients.isNotEmpty()
                    binding.invoiceJob.isEnabled = jobs.isNotEmpty()
                    binding.toggleForm.isEnabled = true
                }
            }
        }
    }

    private fun loadInvoices() {
        listJob?.cancel()
        listState?.loading()
        listJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchInvoices()) {
                is ApiResult.Success -> {
                    showInvoices(result.data)
                    listState?.success(result.data.isEmpty())
                }
                is ApiResult.Error -> listState?.error(result, ::loadInvoices)
            }
        }
    }

    private fun showInvoices(invoices: List<InvoiceResponse>) {
        binding.invoiceList.removeAllViews()
        val statuses = resources.getStringArray(R.array.invoice_statuses)
        invoices.forEach { invoice ->
            val item = layoutInflater.inflate(R.layout.item_invoice, binding.invoiceList, false)
            item.findViewById<TextView>(R.id.invoice_info).text = getString(R.string.invoice_row,
                invoice.number, invoice.clientName ?: getString(R.string.client_name_fallback), formatMoney("ZAR", invoice.total))
            val picker = item.findViewById<Spinner>(R.id.invoice_status)
            // Preserve unknown legacy values on display; loading a row must never update it.
            val localizedStatuses = statuses.map { LocalizedLabels.invoiceStatus(requireContext(), it) }
            val currentStatusLabel = LocalizedLabels.invoiceStatus(requireContext(), invoice.status)
            val rowStatuses = if (invoice.status in statuses) localizedStatuses else listOf(invoice.status) + localizedStatuses
            picker.adapter = spinnerAdapter(rowStatuses)
            picker.setSelection(rowStatuses.indexOf(currentStatusLabel))
            picker.onItemSelectedListener = selectionListener {
                val newStatus = LocalizedLabels.invoiceStatusCode(requireContext(), picker.selectedItem.toString())
                if (newStatus != invoice.status) updateStatus(invoice.id, newStatus, picker)
            }
            binding.invoiceList.addView(item)
        }
    }

    private fun updateStatus(id: String, status: String, picker: Spinner) {
        picker.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = repository.updateInvoiceStatus(id, status)) {
                    is ApiResult.Success -> loadInvoices()
                    is ApiResult.Error -> showInlineError(result) { updateStatus(id, status, picker) }
                }
            } finally { picker.isEnabled = true }
        }
    }

    private fun spinnerAdapter(values: List<String>) = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, values)
    private fun selectionListener(action: () -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
    }
    private fun formatMoney(currency: String, amount: Double) = "$currency ${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}"
    override fun onDestroyView() {
        listJob?.cancel(); clientsJob?.cancel(); jobsJob?.cancel()
        saving = false
        listState = null; _binding = null
        super.onDestroyView()
    }
}
