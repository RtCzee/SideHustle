package com.example.sidehustle.ui.projects

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class ProjectDetailsFragment : PlaceholderFragment() {
    override val titleRes = R.string.project_details_title
    override val descriptionRes = R.string.project_details_placeholder
    override val primaryButtonRes = R.string.action_back

    override fun onPrimaryClicked() {
        findNavController().navigateUp()
    }
}
