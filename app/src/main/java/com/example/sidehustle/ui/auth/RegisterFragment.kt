package com.example.sidehustle.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.CreateProfileRequest
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.FragmentRegisterBinding
import com.example.sidehustle.util.AuthValidator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerButton.setOnClickListener { submitRegistration() }
        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        binding.googleButton.setOnClickListener {
            Snackbar.make(binding.root, R.string.google_sign_in_coming_soon, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun submitRegistration() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()

        binding.nameLayout.error = AuthValidator.nameError(name)?.let { getString(it) }
        binding.emailLayout.error = AuthValidator.emailError(email)?.let { getString(it) }
        binding.passwordLayout.error = AuthValidator.passwordError(password)?.let { getString(it) }
        binding.confirmPasswordLayout.error =
            AuthValidator.confirmPasswordError(password, confirm)?.let { getString(it) }

        if (binding.nameLayout.error != null ||
            binding.emailLayout.error != null ||
            binding.passwordLayout.error != null ||
            binding.confirmPasswordLayout.error != null
        ) {
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d(TAG, "Registered uid=${user?.uid}")
                    if (user == null) {
                        setLoading(false)
                        Snackbar.make(binding.root, R.string.error_register_generic, Snackbar.LENGTH_LONG)
                            .show()
                        return@addOnCompleteListener
                    }

                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profile).addOnCompleteListener {
                        createApiProfile(name)
                    }
                } else {
                    setLoading(false)
                    Log.e(TAG, "Register failed", task.exception)
                    Snackbar.make(binding.root, mapFirebaseError(task.exception), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun createApiProfile(fullName: String) {
        val repository = (requireActivity().application as SideHustleApp).repository

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.createProfile(CreateProfileRequest(fullName = fullName))) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Profile created for uid=${result.data.userId}")
                    setLoading(false)
                    findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
                }
                is ApiResult.Error -> {
                    setLoading(false)
                    Log.e(TAG, "Profile create failed: ${result.message}")
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mapFirebaseError(error: Exception?): String {
        val code = (error as? FirebaseAuthException)?.errorCode
        val messageRes = when (code) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.error_email_in_use
            "ERROR_WEAK_PASSWORD" -> R.string.error_password_short
            "ERROR_INVALID_EMAIL" -> R.string.error_email_invalid
            "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_network
            else -> R.string.error_register_generic
        }
        return getString(messageRes)
    }

    private fun setLoading(loading: Boolean) {
        binding.registerButton.isEnabled = !loading
        binding.googleButton.isEnabled = !loading
        binding.loginLink.isEnabled = !loading
        binding.registerButton.text = getString(
            if (loading) R.string.action_register_loading else R.string.action_register
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "SideHustle"
    }
}
