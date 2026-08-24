package com.example.sidehustle.ui.auth

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class RegisterFragment : PlaceholderFragment() {
    override val titleRes = R.string.register_title
    override val descriptionRes = R.string.register_placeholder
    override val primaryButtonRes = R.string.action_continue_placeholder
    override val secondaryButtonRes = R.string.action_go_to_login

    override fun onPrimaryClicked() {
        findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
    }

    override fun onSecondaryClicked() {
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }
}
