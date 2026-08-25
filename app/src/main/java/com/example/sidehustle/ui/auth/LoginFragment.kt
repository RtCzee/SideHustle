package com.example.sidehustle.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.sidehustle.R
import com.example.sidehustle.databinding.FragmentLoginBinding
import com.example.sidehustle.util.AuthValidator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.setOnClickListener { submitLogin() }
        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.googleButton.setOnClickListener {
            Snackbar.make(binding.root, R.string.google_sign_in_coming_soon, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun submitLogin() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        binding.emailLayout.error = AuthValidator.emailError(email)?.let { getString(it) }
        binding.passwordLayout.error = AuthValidator.passwordError(password)?.let { getString(it) }

        if (binding.emailLayout.error != null || binding.passwordLayout.error != null) {
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "Logged in uid=${auth.currentUser?.uid}")
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                } else {
                    Log.e(TAG, "Login failed", task.exception)
                    Snackbar.make(binding.root, mapFirebaseError(task.exception), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun mapFirebaseError(error: Exception?): String {
        val code = (error as? FirebaseAuthException)?.errorCode
        val messageRes = when (code) {
            "ERROR_INVALID_EMAIL" -> R.string.error_email_invalid
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" -> R.string.error_login_invalid
            "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_network
            else -> R.string.error_login_generic
        }
        return getString(messageRes)
    }

    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        binding.googleButton.isEnabled = !loading
        binding.registerLink.isEnabled = !loading
        binding.loginButton.text = getString(
            if (loading) R.string.action_login_loading else R.string.action_login
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
