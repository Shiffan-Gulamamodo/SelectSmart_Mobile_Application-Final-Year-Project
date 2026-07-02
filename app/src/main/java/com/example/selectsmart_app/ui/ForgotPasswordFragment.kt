package com.example.selectsmart_app.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.selectsmart_app.databinding.FragmentForgotPasswordBinding
import com.example.selectsmart_app.viewmodels.AuthViewModel

// Fragment class used to handle forgot password functionality
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSumbit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (validateEmail(email)) {
                authViewModel.sendPasswordResetEmail(email)
            }
        }
    }

    // Function used to observe reset email result
    private fun setupObservers() {
        authViewModel.resetEmailResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    val email = binding.etEmail.text.toString().trim()
                    Toast.makeText(requireContext(), "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                    val action = ForgotPasswordFragmentDirections.actionForgotPasswordFragmentToVerifyEmailFragment(email)
                    findNavController().navigate(action)
                    authViewModel.resetState() // Clear state after navigating
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function used to validate email input
    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            binding.emailTextInput.error = "Email is required"
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailTextInput.error = "Invalid email address"
            false
        } else {
            binding.emailTextInput.error = null
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
