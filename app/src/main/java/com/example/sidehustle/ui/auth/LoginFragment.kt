package com.example.sidehustle.ui.auth

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class LoginFragment : PlaceholderFragment() {
    override val titleRes = R.string.login_title
    override val descriptionRes = R.string.login_placeholder
    override val primaryButtonRes = R.string.action_continue_placeholder
    override val secondaryButtonRes = R.string.action_go_to_register

    override fun onPrimaryClicked() {
        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    override fun onSecondaryClicked() {
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }
}
