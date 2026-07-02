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
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentRegisterBinding
import com.example.selectsmart_app.models.User
import com.example.selectsmart_app.viewmodels.AuthViewModel

// Fragment class used to handle user registration
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.btnSignUp.setOnClickListener {
            registerUser()
        }

        // Navigate back to Login (Sign in)
        binding.txtLoginAccount.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    // Function used to observe registration status
    private fun setupObservers() {
        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                // Navigate back to login screen after successful registration
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function used to register a new user
    private fun registerUser() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (validateInput(firstName, lastName, email, password, confirmPassword)) {
            val user = User(
                userFirstName = firstName,
                userLastName = lastName,
                userEmail = email
            )
            authViewModel.register(user, password)
        }
    }
    // Function used to validate user input
    private fun validateInput(
        firstName: String,
        lastName: String,
        email: String,
        pass: String,
        confirmPass: String
    ): Boolean {
        var isValid = true

        // Reset errors
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.txtEmail.error = null
        binding.txtPassword.error = null
        binding.txtConfirmPassword.error = null

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "First name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Last name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtEmail.error = "Invalid email address"
            isValid = false
        }

        if (pass.isEmpty()) {
            binding.txtPassword.error = "Password is required"
            isValid = false
        } else if (pass.length < 6) {
            binding.txtPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPass.isEmpty()) {
            binding.txtConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (confirmPass != pass) {
            binding.txtConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}