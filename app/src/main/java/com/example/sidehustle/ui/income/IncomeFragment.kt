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
import com.example.sidehustle.ui.navigation.navigateSafely
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.CreateIncomeRequest
import com.example.sidehustle.data.model.IncomeLinkOption
import com.example.sidehustle.data.model.IncomeOptionsResponse
import com.example.sidehustle.data.model.IncomeResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentIncomeBinding
import com.example.sidehustle.ui.common.list.ListStateHandler
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.util.FormValidator
import com.example.sidehustle.util.LocalizedLabels
import com.example.sidehustle.ui.common.loading.setLoading
import com.example.sidehustle.ui.navigation.setupFinanceNavigation
import com.example.sidehustle.ui.navigation.setupFinanceForm
import com.example.sidehustle.ui.navigation.openFinanceForm
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class IncomeFragment : Fragment() {
    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!
    private var listState: ListStateHandler? = null
    private var loadJob: Job? = null
    private var optionsJob: Job? = null
    private var saving = false
    private var clients = emptyList<IncomeLinkOption>()
    private var jobs = emptyList<IncomeLinkOption>()
    private var invoices = emptyList<IncomeLinkOption>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val paymentMethods = resources.getStringArray(R.array.payment_methods)
        binding.paymentMethod.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf(paymentMethods.first()) + paymentMethods.drop(1).map { LocalizedLabels.paymentMethod(requireContext(), it) })
        binding.incomeDate.setOnClickListener { pickDate() }
        setupFinanceNavigation(view, R.id.incomeFragment)
        setupFinanceForm(view, R.string.action_new_income, ::loadOptions)
        listState = ListStateHandler(this, binding.listState, binding.incomeList,
            R.string.income_empty_title, R.string.income_empty_message, R.string.action_new_income,
            ::openFinanceForm, binding.listRefreshing)
        binding.refreshList.setOnClickListener { loadIncome() }
        binding.saveIncome.setOnClickListener { saveIncome() }
        loadIncome()
    }
    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            binding.incomeDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
            binding.incomeDate.error = null
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun loadOptions() {
        optionsJob?.cancel()
        binding.optionsState.showLoading()
        setOptionInputsEnabled(false)
        optionsJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository().fetchIncomeOptions()) {
                is ApiResult.Success -> {
                    showOptions(result.data)
                    binding.optionsState.isVisible = false
                    setOptionInputsEnabled(true)
                }
                is ApiResult.Error -> binding.optionsState.showError(this@IncomeFragment, result, ::loadOptions)
            }
        }
    }

    private fun setOptionInputsEnabled(enabled: Boolean) {
        binding.incomeClient.isEnabled = enabled
        binding.incomeJob.isEnabled = enabled
        binding.incomeInvoice.isEnabled = enabled
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
        if (saving) return
        dismissInlineError()
        binding.incomeStatus.isVisible = false
        val amountText = binding.incomeAmount.text.toString().trim()
        val date = binding.incomeDate.text.toString().trim()
        val methodLabel = binding.paymentMethod.selectedItem?.toString()
        binding.incomeAmount.error = FormValidator.moneyError(amountText)?.let { getString(it) }
        binding.incomeDate.error = FormValidator.dateError(date)?.let { getString(it) }
        val paymentError = FormValidator.selectionError(methodLabel,
            resources.getStringArray(R.array.payment_methods).drop(1).map { LocalizedLabels.paymentMethod(requireContext(), it) },
            R.string.income_payment_method_error)
        if (paymentError != null) message(getString(paymentError))
        if (binding.incomeAmount.error != null || binding.incomeDate.error != null || paymentError != null) return
        val paymentMethod = method ?: return
        val request = CreateIncomeRequest(amountText.toDouble(), date, paymentMethod, binding.incomeDescription.text.toString().trim().ifBlank { null }, clients.getOrNull(binding.incomeClient.selectedItemPosition - 1)?.id, jobs.getOrNull(binding.incomeJob.selectedItemPosition - 1)?.id, invoices.getOrNull(binding.incomeInvoice.selectedItemPosition - 1)?.id)
        val request = CreateIncomeRequest(amountText.toDouble(), date, LocalizedLabels.paymentMethodCode(requireContext(), methodLabel!!), binding.incomeDescription.text.toString().trim().ifBlank { null }, clients.getOrNull(binding.incomeClient.selectedItemPosition - 1)?.id, jobs.getOrNull(binding.incomeJob.selectedItemPosition - 1)?.id, invoices.getOrNull(binding.incomeInvoice.selectedItemPosition - 1)?.id)
        saving = true
        binding.saveIncome.setLoading(true, R.string.action_saving)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
            when (val result = repository().createIncome(request)) {
                is ApiResult.Success -> { binding.incomeAmount.text?.clear(); binding.incomeDate.text?.clear(); binding.incomeDescription.text?.clear(); message(getString(R.string.income_saved)); loadIncome() }
                is ApiResult.Error -> showInlineError(result, ::saveIncome)
            }
            } finally {
                if (isActive && _binding != null) {
                    saving = false
                    binding.saveIncome.setLoading(false, R.string.action_saving)
                }
            }
        }
    }
    private fun loadIncome() {
        loadJob?.cancel()
        listState?.loading()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository().fetchIncome()) {
                is ApiResult.Success -> {
                    showIncome(result.data)
                    listState?.success(result.data.isEmpty())
                }
                is ApiResult.Error -> listState?.error(result, ::loadIncome)
            }
        }
    }
    private fun showIncome(records: List<IncomeResponse>) {
        binding.incomeList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        records.forEach { record ->
            val item = inflater.inflate(R.layout.item_income, binding.incomeList, false)
            item.findViewById<TextView>(R.id.income_amount_text).text = formatMoney(record.currency, record.amount)
            item.findViewById<TextView>(R.id.income_detail_text).text = listOf(record.dateReceived, LocalizedLabels.paymentMethod(requireContext(), record.paymentMethod), record.description).filterNotNull().filter { it.isNotBlank() }.joinToString(" • ")
            binding.incomeList.addView(item)
        }
    }
    private fun adapter(emptyLabel: String, options: List<IncomeLinkOption>) = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf(emptyLabel) + options.map { it.label })
    private fun repository() = (requireActivity().application as SideHustleApp).repository
    private fun message(value: String) { binding.incomeStatus.text = value; binding.incomeStatus.isVisible = true }
    private fun formatMoney(currency: String, amount: Double) = "$currency ${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}"
    override fun onDestroyView() { loadJob?.cancel(); optionsJob?.cancel(); saving = false; listState = null; _binding = null; super.onDestroyView() }
}
