package com.example.sidehustle.ui.expenses

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
import com.example.sidehustle.util.formatMoney
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.ExpenseResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentExpensesBinding
import com.example.sidehustle.ui.common.list.ListStateHandler
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.util.FormValidator
import com.example.sidehustle.util.LocalizedLabels
import com.example.sidehustle.ui.navigation.setupFinanceNavigation
import com.example.sidehustle.ui.navigation.setupFinanceForm
import com.example.sidehustle.ui.navigation.openFinanceForm
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Capture and display expenses saved by the authenticated user's hosted account. */
class ExpensesFragment : Fragment() {
    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private var listState: ListStateHandler? = null
    private var loadJob: Job? = null
    private var saving = false
    private val categories get() = resources.getStringArray(R.array.expense_categories).drop(1)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categoryLabels = categories.map { LocalizedLabels.expenseCategory(requireContext(), it) }
        binding.categoryInput.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf(resources.getStringArray(R.array.expense_categories).first()) + categoryLabels)
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.saveExpenseButton.setOnClickListener { saveExpense() }
        setupFinanceNavigation(view, R.id.expensesFragment)
        setupFinanceForm(view, R.string.action_new_expense)
        listState = ListStateHandler(this, binding.listState, binding.expensesList,
            R.string.expenses_empty_title, R.string.expenses_empty_message, R.string.action_new_expense,
            ::openFinanceForm, binding.listRefreshing)
        binding.refreshList.setOnClickListener { loadExpenses() }
        loadExpenses()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            binding.dateInput.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
            binding.dateInput.error = null
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveExpense() {
        if (saving) return
        dismissInlineError()
        val amountText = binding.amountInput.text.toString().trim()
        val date = binding.dateInput.text.toString().trim()
        val category = categories.getOrNull(binding.categoryInput.selectedItemPosition - 1)
        binding.expensesStatus.isVisible = false
        binding.amountInput.error = FormValidator.moneyError(amountText)?.let { getString(it) }
        binding.dateInput.error = FormValidator.dateError(date)?.let { getString(it) }
        if (category == null) {
            binding.expensesStatus.text = getString(R.string.expenses_category_error)
            binding.expensesStatus.isVisible = true
        }
        if (binding.amountInput.error != null || binding.dateInput.error != null || category == null) return

        setSaving(true)
        binding.expensesStatus.isVisible = false
        val request = CreateExpenseRequest(
            amount = amountText.toDouble(),
            expenseDate = date,
            category = category,
            description = binding.descriptionInput.text.toString().trim().ifBlank { null },
        )
        val repository = (requireActivity().application as SideHustleApp).repository
        viewLifecycleOwner.lifecycleScope.launch {
            try {
            when (val result = repository.createExpense(request)) {
                is ApiResult.Success -> {
                    binding.amountInput.text?.clear()
                    binding.dateInput.text?.clear()
                    binding.categoryInput.setSelection(0)
                    binding.descriptionInput.text?.clear()
                    binding.expensesStatus.text = getString(R.string.expenses_saved)
                    binding.expensesStatus.isVisible = true
                    loadExpenses()
                    com.example.sidehustle.ui.attachments.AttachmentsDialog.show(
                        parentFragmentManager, "expenses", result.data.expenseId,
                        "${result.data.category} · ${formatMoney(result.data.currency, result.data.amount, useRandSymbol = true)} · ${result.data.expenseDate}")
                }
                is ApiResult.Error -> showInlineError(result, ::saveExpense)
            }
            } finally {
                if (isActive && _binding != null) setSaving(false)
            }
        }
    }

    private fun loadExpenses() {
        loadJob?.cancel()
        listState?.loading()
        val repository = (requireActivity().application as SideHustleApp).repository
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchExpenses()) {
                is ApiResult.Success -> {
                    showExpenses(result.data)
                    listState?.success(result.data.isEmpty())
                }
                is ApiResult.Error -> listState?.error(result, ::loadExpenses)
            }
        }
    }
    private fun showExpenses(expenses: List<ExpenseResponse>) {
        binding.expensesList.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        expenses.forEach { expense ->
            val item = inflater.inflate(R.layout.item_expense, binding.expensesList, false)
            val categoryLabel = LocalizedLabels.expenseCategory(requireContext(), expense.category)
            item.findViewById<TextView>(R.id.expense_category).text = categoryLabel
            item.findViewById<TextView>(R.id.expense_amount).text = formatMoney(expense.currency, expense.amount, useRandSymbol = true)
            item.findViewById<TextView>(R.id.expense_date).text = expense.expenseDate
            item.findViewById<TextView>(R.id.expense_description).apply {
                text = expense.description
                isVisible = !expense.description.isNullOrBlank()
            }
            binding.expensesList.addView(item)
            item.isClickable = true
            item.isFocusable = true
            item.contentDescription = getString(R.string.attachment_expense_details, categoryLabel)
            item.setOnClickListener {
                com.example.sidehustle.ui.attachments.AttachmentsDialog.show(
                    parentFragmentManager, "expenses", expense.expenseId,
                    listOfNotNull(categoryLabel, formatMoney(expense.currency, expense.amount, useRandSymbol = true),
                        expense.expenseDate, expense.description).joinToString("\n"))
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        this.saving = saving
        binding.saveExpenseButton.isEnabled = !saving
        binding.saveExpenseButton.text = getString(if (saving) R.string.expenses_saving else R.string.expenses_save)
    }


    override fun onDestroyView() {
        loadJob?.cancel()
        saving = false
        listState = null
        _binding = null
        super.onDestroyView()
    }
}
