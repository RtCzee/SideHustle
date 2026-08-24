package com.example.sidehustle.ui.clients

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class ClientsFragment : PlaceholderFragment() {
    override val titleRes = R.string.clients_title
    override val descriptionRes = R.string.clients_placeholder
    override val primaryButtonRes = R.string.action_open_client_details

    override fun onPrimaryClicked() {
        findNavController().navigate(R.id.action_clientsFragment_to_clientDetailsFragment)
    }
}
