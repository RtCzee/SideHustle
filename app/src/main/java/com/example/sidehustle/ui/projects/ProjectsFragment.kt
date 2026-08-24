package com.example.sidehustle.ui.projects

import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.ui.common.PlaceholderFragment

class ProjectsFragment : PlaceholderFragment() {
    override val titleRes = R.string.projects_title
    override val descriptionRes = R.string.projects_placeholder
    override val primaryButtonRes = R.string.action_open_project_details

    override fun onPrimaryClicked() {
        findNavController().navigate(R.id.action_projectsFragment_to_projectDetailsFragment)
    }
}
