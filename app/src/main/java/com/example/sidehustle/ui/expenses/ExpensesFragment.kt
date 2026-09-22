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
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.CreateExpenseRequest
import com.example.sidehustle.data.model.ExpenseResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentExpensesBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/** Capture and display expenses saved by the authenticated user's hosted account. */
class ExpensesFragment : Fragment() {
    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.categoryInput.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.expense_categories, android.R.layout.simple_spinner_dropdown_item,
        )
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.saveExpenseButton.setOnClickListener { saveExpense() }
        binding.invoicesTab.setOnClickListener { findNavController().navigateUp() }
        binding.incomeTab.setOnClickListener { findNavController().navigate(R.id.action_expensesFragment_to_incomeFragment) }
        loadExpenses()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            binding.dateInput.setText("%04d-%02d-%02d".format(year, month + 1, day))
            binding.dateInput.error = null
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveExpense() {
        val amount = binding.amountInput.text.toString().trim().toDoubleOrNull()
        val date = binding.dateInput.text.toString().trim()
        val category = binding.categoryInput.selectedItem?.toString().orEmpty()
        var valid = true
        if (amount == null || !amount.isFinite() || amount <= 0) {
            binding.amountInput.error = getString(R.string.expenses_amount_error)
            valid = false
        } else binding.amountInput.error = null
        if (date.isBlank()) {
            binding.dateInput.error = getString(R.string.expenses_date_error)
            valid = false
        }
        if (category == getString(R.string.expenses_category_prompt)) {
            binding.expensesStatus.text = getString(R.string.expenses_category_error)
            binding.expensesStatus.isVisible = true
            valid = false
        }
        if (!valid) return

        setSaving(true)
        binding.expensesStatus.isVisible = false
        val request = CreateExpenseRequest(
            amount = amount ?: return,
            expenseDate = date,
            category = category,
            description = binding.descriptionInput.text.toString().trim().ifBlank { null },
        )
        val repository = (requireActivity().application as SideHustleApp).repository
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.createExpense(request)) {
                is ApiResult.Success -> {
                    binding.amountInput.text?.clear()
                    binding.dateInput.text?.clear()
                    binding.categoryInput.setSelection(0)
                    binding.descriptionInput.text?.clear()
                    binding.expensesStatus.text = getString(R.string.expenses_saved)
                    binding.expensesStatus.isVisible = true
                    loadExpenses()
                }
                is ApiResult.Error -> showError(result.message)
            }
            setSaving(false)
        }
    }

    private fun loadExpenses() {
        val repository = (requireActivity().application as SideHustleApp).repository
        binding.expensesLoading.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchExpenses()) {
                is ApiResult.Success -> showExpenses(result.data)
                is ApiResult.Error -> showError(result.message)
            }
            binding.expensesLoading.isVisible = false
        }
    }

    private fun showExpenses(expenses: List<ExpenseResponse>) {
        binding.expensesList.removeAllViews()
        binding.expensesEmpty.isVisible = expenses.isEmpty()
        val inflater = LayoutInflater.from(requireContext())
        expenses.forEach { expense ->
            val item = inflater.inflate(R.layout.item_expense, binding.expensesList, false)
            item.findViewById<TextView>(R.id.expense_category).text = expense.category
            item.findViewById<TextView>(R.id.expense_amount).text = formatMoney(expense.currency, expense.amount)
            item.findViewById<TextView>(R.id.expense_date).text = expense.expenseDate
            item.findViewById<TextView>(R.id.expense_description).apply {
                text = expense.description
                isVisible = !expense.description.isNullOrBlank()
            }
            binding.expensesList.addView(item)
        }
    }

    private fun showError(message: String) {
        binding.expensesStatus.text = message
        binding.expensesStatus.isVisible = true
    }

    private fun setSaving(saving: Boolean) {
        binding.saveExpenseButton.isEnabled = !saving
        binding.saveExpenseButton.text = getString(if (saving) R.string.expenses_saving else R.string.expenses_save)
    }

    private fun formatMoney(currency: String, amount: Double): String {
        val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
        return if (currency == "ZAR") "R $formatted" else "$currency $formatted"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
