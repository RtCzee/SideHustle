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
import com.example.sidehustle.databinding.FragmentInvoicesBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class InvoicesFragment : Fragment() {
    private var _binding: FragmentInvoicesBinding? = null
    private val binding get() = _binding!!
    private var clients = emptyList<InvoiceClient>()
    private var jobs = emptyList<InvoiceJob>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentInvoicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.expensesTab.setOnClickListener { findNavController().navigate(R.id.action_invoicesFragment_to_expensesFragment) }
        binding.incomeTab.setOnClickListener { findNavController().navigate(R.id.action_invoicesFragment_to_incomeFragment) }
        binding.invoiceClient.onItemSelectedListener = selectionListener { loadJobsForSelectedClient() }
        binding.invoiceJob.onItemSelectedListener = selectionListener { showSelectedTotal() }
        binding.createInvoice.setOnClickListener { createInvoice() }
        loadClients()
        loadInvoices()
    }

    private fun loadClients() = viewLifecycleOwner.lifecycleScope.launch {
        when (val result = repository().fetchInvoiceClients()) {
            is ApiResult.Success -> {
                clients = result.data
                binding.invoiceClient.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_client)) + clients.map { it.name })
            }
            is ApiResult.Error -> showMessage(result.message)
        }
    }

    private fun loadJobsForSelectedClient() {
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1) ?: run {
            jobs = emptyList()
            binding.invoiceJob.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_job)))
            showSelectedTotal()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository().fetchInvoiceJobs(client.id)) {
                is ApiResult.Success -> {
                    jobs = result.data
                    binding.invoiceJob.adapter = spinnerAdapter(listOf(getString(R.string.invoice_select_job)) + jobs.map { it.title })
                    showSelectedTotal()
                }
                is ApiResult.Error -> showMessage(result.message)
            }
        }
    }

    private fun showSelectedTotal() {
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        binding.invoiceTotal.text = job?.let { formatMoney(it.currency, it.agreedAmount) } ?: getString(R.string.invoice_total_placeholder)
    }

    private fun createInvoice() {
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1)
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        if (client == null || job == null) { showMessage(getString(R.string.invoice_selection_error)); return }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository().createInvoice(CreateInvoiceRequest(client.id, job.id))) {
                is ApiResult.Success -> { showMessage(getString(R.string.invoice_created)); loadInvoices() }
                is ApiResult.Error -> showMessage(result.message)
            }
        }
    }

    private fun loadInvoices() = viewLifecycleOwner.lifecycleScope.launch {
        when (val result = repository().fetchInvoices()) {
            is ApiResult.Success -> showInvoices(result.data)
            is ApiResult.Error -> showMessage(result.message)
        }
    }

    private fun showInvoices(invoices: List<InvoiceResponse>) {
        binding.invoiceList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val statuses = resources.getStringArray(R.array.invoice_statuses)
        invoices.forEach { invoice ->
            val item = inflater.inflate(R.layout.item_invoice, binding.invoiceList, false)
            item.findViewById<TextView>(R.id.invoice_info).text = "${invoice.number} • ${invoice.clientName ?: "Client"} • ${formatMoney("ZAR", invoice.total)}"
            val statusPicker = item.findViewById<Spinner>(R.id.invoice_status)
            statusPicker.adapter = spinnerAdapter(statuses.toList())
            statusPicker.setSelection(statuses.indexOf(invoice.status).coerceAtLeast(0))
            statusPicker.onItemSelectedListener = selectionListener {
                val newStatus = statusPicker.selectedItem.toString()
                if (newStatus != invoice.status) updateStatus(invoice.id, newStatus)
            }
            binding.invoiceList.addView(item)
        }
    }

    private fun updateStatus(id: String, status: String) = viewLifecycleOwner.lifecycleScope.launch {
        when (val result = repository().updateInvoiceStatus(id, status)) {
            is ApiResult.Success -> loadInvoices()
            is ApiResult.Error -> showMessage(result.message)
        }
    }

    private fun spinnerAdapter(values: List<String>) = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, values)
    private fun selectionListener(action: () -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
    }
    private fun repository() = (requireActivity().application as SideHustleApp).repository
    private fun showMessage(message: String) { binding.invoiceMessage.text = message; binding.invoiceMessage.isVisible = true }
    private fun formatMoney(currency: String, amount: Double) = "$currency ${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}"
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
