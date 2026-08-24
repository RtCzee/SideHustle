package com.example.sidehustle.ui.clients

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class ClientDetailsFragment : PlaceholderFragment() {
    override val titleRes = R.string.client_details_title
    override val descriptionRes = R.string.client_details_placeholder
    override val primaryButtonRes = R.string.action_back

    override fun onPrimaryClicked() {
        findNavController().navigateUp()
    }
}
