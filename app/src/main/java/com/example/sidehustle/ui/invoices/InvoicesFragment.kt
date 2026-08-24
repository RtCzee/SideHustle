package com.example.sidehustle.ui.invoices

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class InvoicesFragment : PlaceholderFragment() {
    override val titleRes = R.string.invoices_title
    override val descriptionRes = R.string.invoices_placeholder
    override val primaryButtonRes = R.string.action_open_expenses

    override fun onPrimaryClicked() {
        findNavController().navigate(R.id.action_invoicesFragment_to_expensesFragment)
    }
}
