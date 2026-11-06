package com.example.sidehustle.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.ui.navigation.resetSession
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.UpdateProfileRequest
import com.example.sidehustle.data.model.UserProfileResponse
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentSettingsBinding
import com.example.sidehustle.util.AuthValidator
import com.example.sidehustle.util.FormValidator
import com.example.sidehustle.ui.common.error.dismissInlineError
import com.example.sidehustle.ui.common.error.showInlineError
import com.example.sidehustle.ui.common.error.showFullPageError
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var loadJob: Job? = null
    private var saving = false

    private lateinit var currencyCodes: List<String>
    private lateinit var currencyLabels: List<String>
    private lateinit var languageCodes: List<String>
    private lateinit var languageLabels: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyCodes = resources.getStringArray(R.array.settings_currency_codes).toList()
        currencyLabels = resources.getStringArray(R.array.settings_currency_labels).toList()
        languageCodes = resources.getStringArray(R.array.settings_language_codes).toList()
        languageLabels = resources.getStringArray(R.array.settings_language_labels).toList()

        binding.currencyInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, currencyLabels)
        )
        binding.languageInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, languageLabels)
        )

        val email = auth.currentUser?.email
        binding.signedInLabel.text = if (email != null) {
            getString(R.string.settings_signed_in_as, email)
        } else {
            getString(R.string.settings_not_signed_in)
        }

        binding.saveButton.setOnClickListener { submitSave() }
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            Log.d(TAG, "User signed out")
            Snackbar.make(binding.root, R.string.logged_out_message, Snackbar.LENGTH_SHORT).show()
            findNavController().resetSession(R.id.loginFragment)
        }

        loadProfile()
    }

    private fun loadProfile() {
        loadJob?.cancel()
        dismissInlineError()
        val repository = (requireActivity().application as SideHustleApp).repository
        binding.errorState.isVisible = false
        binding.formGroup.isVisible = false
        binding.loadingIndicator.isVisible = true

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.fetchProfile()) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Settings loaded for ${result.data.userId}")
                    populateFields(result.data)
                    (requireActivity().application as SideHustleApp).pushRegistration
                        .setEnabled(result.data.userId, result.data.notificationsEnabled)
                    binding.loadingIndicator.isVisible = false
                    binding.formGroup.isVisible = true
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Settings load failed: ${result.message}")
                    binding.loadingIndicator.isVisible = false
                    showFullPageError(binding.errorState, result, ::loadProfile)
                }
            }
        }
    }

    private fun populateFields(profile: UserProfileResponse) {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            applyLanguage(profile.preferredLanguage)
        }
        binding.nameInput.setText(profile.fullName)
        binding.phoneInput.setText(profile.phoneNumber.orEmpty())

        val currencyIndex = currencyCodes.indexOf(profile.preferredCurrency).takeIf { it >= 0 } ?: 0
        binding.currencyInput.setText(currencyLabels[currencyIndex], false)

        val languageIndex = languageCodes.indexOf(profile.preferredLanguage).takeIf { it >= 0 } ?: 0
        binding.languageInput.setText(languageLabels[languageIndex], false)

        binding.notificationsSwitch.isChecked = profile.notificationsEnabled
        binding.nameLayout.error = null
        binding.phoneLayout.error = null
    }

    private fun submitSave() {
        if (saving) return
        dismissInlineError()
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()

        binding.nameLayout.error = AuthValidator.nameError(name)?.let { getString(it) }
        binding.phoneLayout.error = AuthValidator.phoneError(phone)?.let { getString(it) }
        val currency = binding.currencyInput.text.toString()
        val language = binding.languageInput.text.toString()
        binding.currencyLayout.error = FormValidator.selectionError(currency, currencyLabels, R.string.error_currency_required)?.let { getString(it) }
        binding.languageLayout.error = FormValidator.selectionError(language, languageLabels, R.string.error_language_required)?.let { getString(it) }

        if (binding.nameLayout.error != null || binding.phoneLayout.error != null ||
            binding.currencyLayout.error != null || binding.languageLayout.error != null) {
            return
        }

        val currencyCode = currencyCodes[currencyLabels.indexOf(currency)]
        val languageCode = languageCodes[languageLabels.indexOf(language)]

        val request = UpdateProfileRequest(
            fullName = name,
            phoneNumber = phone,
            preferredCurrency = currencyCode,
            preferredLanguage = languageCode,
            notificationsEnabled = binding.notificationsSwitch.isChecked,
        )

        val repository = (requireActivity().application as SideHustleApp).repository
        setSaving(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
            when (val result = repository.updateProfile(request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Settings saved for ${result.data.userId}")
                    (requireActivity().application as SideHustleApp).pushRegistration
                        .setEnabled(result.data.userId, result.data.notificationsEnabled)
                    applyLanguage(result.data.preferredLanguage)
                    (requireActivity() as com.example.sidehustle.MainActivity)
                        .syncPushRegistration(requestPermission = result.data.notificationsEnabled)
                    populateFields(result.data)
                    setSaving(false)
                    Snackbar.make(binding.root, R.string.settings_saved_message, Snackbar.LENGTH_SHORT)
                        .show()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Settings save failed: ${result.message}")
                    setSaving(false)
                    showInlineError(result, ::submitSave)
                }
            }
            } finally {
                if (isActive && _binding != null) setSaving(false)
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        this.saving = saving
        binding.saveButton.isEnabled = !saving
        binding.logoutButton.isEnabled = !saving
        binding.saveButton.text = getString(
            if (saving) R.string.action_save_loading else R.string.action_save
        )
    }

    private fun applyLanguage(languageCode: String) {
        val supported = setOf("en", "zu", "af")
        val locale = languageCode.substringBefore('-').lowercase().takeIf { it in supported }.orEmpty()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        saving = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
