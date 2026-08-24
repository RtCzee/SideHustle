package com.example.sidehustle.ui.expenses

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class ExpensesFragment : PlaceholderFragment() {
    override val titleRes = R.string.expenses_title
    override val descriptionRes = R.string.expenses_placeholder
    override val primaryButtonRes = R.string.action_back

    override fun onPrimaryClicked() {
        findNavController().navigateUp()
    }
}
