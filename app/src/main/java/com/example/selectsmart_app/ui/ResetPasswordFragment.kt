package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentResetPasswordBinding
import com.example.selectsmart_app.viewmodels.AuthViewModel

// Fragment class used to reset user password
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    // Stores the Firebase password reset code from the reset link.
    // This code is needed to confirm that the reset request is valid.
    private var oobCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve oobCode from arguments (deep link)
        oobCode = arguments?.getString("oobCode")

        if (oobCode == null) {
            Toast.makeText(requireContext(), "Invalid or expired reset link", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
            return
        }

        setupObservers()

        binding.btnReset.setOnClickListener {
            resetPassword()
        }
    }

    // Function used to observe password reset result
    private fun setupObservers() {
        authViewModel.confirmResetResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Password reset successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_resetPasswordFragment_to_loginFragment)
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function used to validate and reset user password
    private fun resetPassword() {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Clear previous errors at the start of validation
        binding.txtPassword.error = null
        binding.txtConfirmPassword.error = null

        var isValid = true

        if (password.isEmpty()) {
            binding.txtPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.txtPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.txtConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.txtConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (isValid) {
            oobCode?.let {
                authViewModel.confirmPasswordReset(it, password)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
