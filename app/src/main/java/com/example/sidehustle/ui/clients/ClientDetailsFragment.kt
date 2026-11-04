package com.example.sidehustle.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.ClientRequest
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentClientDetailsBinding
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showFullPageError
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.loading.setLoading
import com.example.sidehustle.ui.navigation.isValidDetailRequest
import com.example.sidehustle.util.AuthValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The visible Add and row actions in Clients both lead here, with explicit create/edit intent. */
class ClientDetailsFragment : Fragment() {
    private var _binding: FragmentClientDetailsBinding? = null
    private val binding get() = _binding!!
    private val clientId get() = arguments?.getString(ARG_CLIENT_ID)
    private val creating get() = arguments?.getBoolean("isCreating") == true
    private val repository get() = (requireActivity().application as SideHustleApp).repository
    private var request: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.setText(if (creating) R.string.client_details_add_title else R.string.client_details_edit_title)
        binding.saveButton.setOnClickListener { save() }
        binding.deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.dialog_delete_client_title)
                .setMessage(getString(R.string.dialog_delete_client_message, binding.nameInput.text.toString()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ -> delete() }.show()
        }
        load()
    }

    private fun load() {
        request?.cancel()
        binding.errorState.isVisible = false
        binding.formGroup.isVisible = false
        if (!isValidDetailRequest(clientId, creating)) {
            showFullPageError(binding.errorState, ApiResult.Error("", 404), ::load)
        } else if (creating) {
            binding.formGroup.isVisible = true
        } else {
            binding.loadingIndicator.isVisible = true
            request = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    when (val result = repository.fetchClient(clientId!!)) {
                        is ApiResult.Success -> {
                            bind(result.data)
                            binding.formGroup.isVisible = true
                            binding.deleteButton.isVisible = true
                        }
                        is ApiResult.Error -> showFullPageError(binding.errorState, result, ::load)
                    }
                } finally {
                    if (isActive) _binding?.loadingIndicator?.isVisible = false
                }
            }
        }
    }

    private fun bind(client: ClientResponse) {
        binding.nameInput.setText(client.name)
        binding.emailInput.setText(client.email.orEmpty())
        binding.phoneInput.setText(client.phoneNumber.orEmpty())
        binding.addressInput.setText(client.address.orEmpty())
        binding.notesInput.setText(client.notes.orEmpty())
    }

    private fun save() {
        if (request?.isActive == true) return
        dismissInlineError()
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        binding.nameLayout.error = AuthValidator.nameError(name)?.let { getString(it) }
        binding.emailLayout.error = AuthValidator.optionalEmailError(email)?.let { getString(it) }
        binding.phoneLayout.error = AuthValidator.phoneError(phone)?.let { getString(it) }
        if (binding.nameLayout.error != null || binding.emailLayout.error != null || binding.phoneLayout.error != null) return
        val data = ClientRequest(name, email, phone, binding.addressInput.text.toString().trim(), binding.notesInput.text.toString().trim())
        binding.saveButton.setLoading(true, R.string.action_saving)
        binding.deleteButton.isEnabled = false
        request = viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = if (creating) repository.createClient(data) else repository.updateClient(clientId!!, data)) {
                    is ApiResult.Success -> findNavController().navigateUp()
                    is ApiResult.Error -> showInlineError(result, ::save)
                }
            } finally {
                if (isActive) _binding?.let { it.saveButton.setLoading(false, R.string.action_saving); it.deleteButton.isEnabled = true }
            }
        }
    }

    private fun delete() {
        if (request?.isActive == true) return
        dismissInlineError()
        val id = clientId ?: return
        binding.deleteButton.isEnabled = false
        binding.saveButton.isEnabled = false
        request = viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (val result = repository.deleteClient(id)) {
                    is ApiResult.Success -> findNavController().navigateUp()
                    is ApiResult.Error -> showInlineError(result, ::delete)
                }
            } finally {
                if (isActive) _binding?.let { it.deleteButton.isEnabled = true; it.saveButton.isEnabled = true }
            }
        }
    }

    override fun onDestroyView() {
        request?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object { const val ARG_CLIENT_ID = "clientId" }
}
