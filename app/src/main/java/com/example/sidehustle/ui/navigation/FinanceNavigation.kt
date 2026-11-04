package com.example.sidehustle.ui.navigation

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.google.android.material.button.MaterialButton

fun Fragment.setupFinanceNavigation(root: View, selected: Int) {
    listOf(R.id.invoices_tab to R.id.invoicesFragment, R.id.expenses_tab to R.id.expensesFragment,
        R.id.income_tab to R.id.incomeFragment).forEach { (viewId, destination) ->
        root.findViewById<View>(viewId).apply {
            isSelected = destination == selected
            setOnClickListener {
                if (findNavController().currentDestination?.id != destination) {
                    findNavController().navigateSafely(destination, options = NavOptions.Builder()
                        .setPopUpTo(R.id.invoicesFragment, false).setLaunchSingleTop(true).build())
                }
            }
        }
    }
}

/** An explicit Add action opens the form; list states are visible immediately on a phone. */
fun Fragment.setupFinanceForm(root: View, @StringRes openLabel: Int, loadOptions: () -> Unit = {}) {
    val form = root.findViewById<View>(R.id.entry_form)
    val toggle = root.findViewById<MaterialButton>(R.id.toggle_form)
    toggle.setOnClickListener {
        form.isVisible = !form.isVisible
        toggle.setText(if (form.isVisible) R.string.action_hide_form else openLabel)
        if (form.isVisible) {
            loadOptions()
            root.post { (root as? android.widget.ScrollView)?.smoothScrollTo(0, 0) }
        }
    }
}

fun Fragment.openFinanceForm() {
    val root = view ?: return
    if (!root.findViewById<View>(R.id.entry_form).isVisible) root.findViewById<View>(R.id.toggle_form).performClick()
    root.post { (root as? android.widget.ScrollView)?.smoothScrollTo(0, 0) }
}