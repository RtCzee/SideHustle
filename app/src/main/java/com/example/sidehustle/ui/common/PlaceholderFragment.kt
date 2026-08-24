package com.example.sidehustle.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.sidehustle.R
import com.example.sidehustle.databinding.FragmentPlaceholderBinding
import com.google.android.material.button.MaterialButton

open class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    protected val binding get() = _binding!!

    protected open val titleRes: Int = R.string.app_name
    protected open val descriptionRes: Int = R.string.placeholder_body
    protected open val primaryButtonRes: Int? = null
    protected open val secondaryButtonRes: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.setText(titleRes)
        binding.description.setText(descriptionRes)
        bindButton(binding.primaryButton, primaryButtonRes) { onPrimaryClicked() }
        bindButton(binding.secondaryButton, secondaryButtonRes) { onSecondaryClicked() }
    }

    protected open fun onPrimaryClicked() = Unit

    protected open fun onSecondaryClicked() = Unit

    private fun bindButton(button: MaterialButton, textRes: Int?, onClick: () -> Unit) {
        if (textRes == null) {
            button.isVisible = false
        } else {
            button.isVisible = true
            button.setText(textRes)
            button.setOnClickListener { onClick() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
