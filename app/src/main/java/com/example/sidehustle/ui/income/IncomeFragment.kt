package com.example.sidehustle.ui.income

import android.app.DatePickerDialog
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
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.IncomeLinkOption
import com.example.sidehustle.data.model.IncomeOptionsResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentIncomeBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class IncomeFragment : Fragment() {
    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!
    private var clients = emptyList<IncomeLinkOption>()
    private var jobs = emptyList<IncomeLinkOption>()
    private var invoices = emptyList<IncomeLinkOption>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        binding.paymentMethod.adapter = ArrayAdapter.createFromResource(requireContext(), R.array.payment_methods, android.R.layout.simple_spinner_dropdown_item)
        binding.incomeDate.setOnClickListener { pickDate() }
        binding.invoicesTab.setOnClickListener { findNavController().navigate(R.id.action_incomeFragment_to_invoicesFragment) }
        binding.expensesTab.setOnClickListener { findNavController().navigate(R.id.action_incomeFragment_to_expensesFragment) }
        binding.saveIncome.setOnClickListener { saveIncome() }
        loadOptions(); loadIncome()
    }
    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d -> binding.incomeDate.setText("%04d-%02d-%02d".format(y, m + 1, d)) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun loadOptions() = viewLifecycleOwner.lifecycleScope.launch {
        when (val result = repository().fetchIncomeOptions()) {
            is ApiResult.Success -> showOptions(result.data)
            is ApiResult.Error -> message(result.message)
        }
    }
    private fun showOptions(options: IncomeOptionsResponse) {
        clients = options.clients.map { IncomeLinkOption(it.id, it.name) }
        jobs = options.jobs.map { IncomeLinkOption(it.id, it.title) }
        invoices = options.invoices.map { IncomeLinkOption(it.id, it.number) }
        binding.incomeClient.adapter = adapter(getString(R.string.income_no_client), clients)
        binding.incomeJob.adapter = adapter(getString(R.string.income_no_job), jobs)
        binding.incomeInvoice.adapter = adapter(getString(R.string.income_no_invoice), invoices)
    }
    private fun saveIncome() {
        val amount = binding.incomeAmount.text.toString().trim().toDoubleOrNull()
        val date = binding.incomeDate.text.toString().trim()
        if (amount == null || !amount.isFinite() || amount <= 0) { binding.incomeAmount.error = getString(R.string.income_amount_error); return }
        if (date.isBlank()) { binding.incomeDate.error = getString(R.string.income_date_error); return }
        if (binding.paymentMethod.selectedItemPosition == 0) { message(getString(R.string.income_payment_method_error)); return }
        val request = CreateIncomeRequest(amount, date, binding.paymentMethod.selectedItem.toString(), binding.incomeDescription.text.toString().trim().ifBlank { null }, clients.getOrNull(binding.incomeClient.selectedItemPosition - 1)?.id, jobs.getOrNull(binding.incomeJob.selectedItemPosition - 1)?.id, invoices.getOrNull(binding.incomeInvoice.selectedItemPosition - 1)?.id)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository().createIncome(request)) {
                is ApiResult.Success -> { binding.incomeAmount.text?.clear(); binding.incomeDate.text?.clear(); binding.incomeDescription.text?.clear(); message(getString(R.string.income_saved)); loadIncome() }
                is ApiResult.Error -> message(result.message)
            }
        }
    }
    private fun loadIncome() = viewLifecycleOwner.lifecycleScope.launch {
        when (val result = repository().fetchIncome()) { is ApiResult.Success -> showIncome(result.data); is ApiResult.Error -> message(result.message) }
    }
    private fun showIncome(records: List<IncomeResponse>) {
        binding.incomeList.removeAllViews(); binding.incomeEmpty.isVisible = records.isEmpty()
        val inflater = LayoutInflater.from(requireContext())
        records.forEach { record ->
            val item = inflater.inflate(R.layout.item_income, binding.incomeList, false)
            item.findViewById<TextView>(R.id.income_amount_text).text = formatMoney(record.currency, record.amount)
            item.findViewById<TextView>(R.id.income_detail_text).text = listOf(record.dateReceived, record.paymentMethod, record.description).filterNotNull().filter { it.isNotBlank() }.joinToString(" • ")
            binding.incomeList.addView(item)
        }
    }
    private fun adapter(emptyLabel: String, options: List<IncomeLinkOption>) = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(emptyLabel) + options.map { it.label })
    private fun repository() = (requireActivity().application as SideHustleApp).repository
    private fun message(value: String) { binding.incomeStatus.text = value; binding.incomeStatus.isVisible = true }
    private fun formatMoney(currency: String, amount: Double) = "$currency ${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}"
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
