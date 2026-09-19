package com.example.sidehustle.ui.invoices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

class InvoicesFragment : Fragment() {
    private var _binding: FragmentInvoicesBinding? = null
    private val binding get() = _binding!!
    private var clients = emptyList<InvoiceClient>()
    private var jobs = emptyList<InvoiceJob>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentInvoicesBinding.inflate(inflater, container, false); return binding.root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        binding.invoiceClient.onItemSelectedListener = simpleSelection { loadJobs() }
        binding.invoiceJob.onItemSelectedListener = simpleSelection { showSelectedTotal() }
        binding.createInvoice.setOnClickListener { createInvoice() }
        binding.expensesTab.setOnClickListener {
            findNavController().navigate(R.id.action_invoicesFragment_to_expensesFragment)
        }
        loadClients(); loadInvoices()
    }
    private fun loadClients() = viewLifecycleOwner.lifecycleScope.launch {
        val repo = (requireActivity().application as SideHustleApp).repository
        when (val result = repo.fetchInvoiceClients()) {
            is ApiResult.Success -> { clients = result.data; binding.invoiceClient.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.invoice_select_client)) + clients.map { it.name }) }
            is ApiResult.Error -> message(result.message)
        }
    }
    private fun loadJobs() {
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = (requireActivity().application as SideHustleApp).repository
            when (val result = repo.fetchInvoiceJobs(client.id)) {
                is ApiResult.Success -> { jobs = result.data; binding.invoiceJob.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.invoice_select_job)) + jobs.map { it.title }); showSelectedTotal() }
                is ApiResult.Error -> message(result.message)
            }
        }
    }
    private fun showSelectedTotal() {
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        binding.invoiceTotal.text = job?.let { "Total: ${it.currency} ${NumberFormat.getNumberInstance().format(it.agreedAmount)}" } ?: getString(R.string.invoice_total_placeholder)
    }
    private fun createInvoice() {
        val client = clients.getOrNull(binding.invoiceClient.selectedItemPosition - 1)
        val job = jobs.getOrNull(binding.invoiceJob.selectedItemPosition - 1)
        if (client == null || job == null) { message(getString(R.string.invoice_select_error)); return }
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = (requireActivity().application as SideHustleApp).repository
            when (val result = repo.createInvoice(CreateInvoiceRequest(client.id, job.id))) {
                is ApiResult.Success -> { message(getString(R.string.invoice_saved)); loadInvoices() }
                is ApiResult.Error -> message(result.message)
            }
        }
    }
    private fun loadInvoices() = viewLifecycleOwner.lifecycleScope.launch {
        val repo = (requireActivity().application as SideHustleApp).repository
        when (val result = repo.fetchInvoices()) { is ApiResult.Success -> showInvoices(result.data); is ApiResult.Error -> message(result.message) }
    }
    private fun showInvoices(invoices: List<InvoiceResponse>) {
        binding.invoiceList.removeAllViews(); val inflater = LayoutInflater.from(requireContext())
        invoices.forEach { invoice ->
            val item = inflater.inflate(R.layout.item_invoice, binding.invoiceList, false)
            item.findViewById<TextView>(R.id.invoice_info).text = "${invoice.number} • ${invoice.clientName ?: "Client"} • ${NumberFormat.getNumberInstance().format(invoice.total)}"
            val status = item.findViewById<android.widget.Spinner>(R.id.invoice_status)
            status.adapter = ArrayAdapter.createFromResource(requireContext(), R.array.invoice_statuses, android.R.layout.simple_spinner_dropdown_item)
            status.setSelection(resources.getStringArray(R.array.invoice_statuses).indexOf(invoice.status).coerceAtLeast(0))
            status.onItemSelectedListener = simpleSelection { val selected = status.selectedItem.toString(); if (selected != invoice.status) updateStatus(invoice.id, selected) }
            binding.invoiceList.addView(item)
        }
    }
    private fun updateStatus(id: String, status: String) = viewLifecycleOwner.lifecycleScope.launch { val repo = (requireActivity().application as SideHustleApp).repository; when (val r = repo.updateInvoiceStatus(id, status)) { is ApiResult.Success -> loadInvoices(); is ApiResult.Error -> message(r.message) } }
    private fun message(text: String) { binding.invoiceMessage.text = text; binding.invoiceMessage.isVisible = true }
    private fun simpleSelection(action: () -> Unit) = object : android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit; override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = action() }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
