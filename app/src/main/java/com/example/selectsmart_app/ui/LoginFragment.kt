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
import com.example.selectsmart_app.databinding.FragmentLoginBinding
import com.example.selectsmart_app.viewmodels.AuthViewModel

// This fragment handles the login screen of the application.
// It allows the user to enter their email and password,
// validates the input, and then uses AuthViewModel to log the user in.
class LoginFragment : Fragment() {

    // ViewBinding variable for fragment_login.xml.
    // The nullable version is used so it can be cleared when the view is destroyed.
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Creates an instance of AuthViewModel.
    // This ViewModel handles the login logic and communicates with AuthRepository/Firebase.
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // This function runs after the view has been created.
    // It is used to set up button clicks and observers.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.btnSignIn.setOnClickListener {
            loginUser()
        }

        binding.txtCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    // Function used to observe login result from AuthViewModel.
    private fun setupObservers() {
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Login Failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function used to handle user login
    private fun loginUser() {
        // Gets the email from the email input field and removes extra spaces.
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (validateInput(email, password)) {
            authViewModel.login(email, password)
        }
    }

    // Function used to validate user input
    private fun validateInput(email: String, pass: String): Boolean {
        var isValid = true
        
        // Clear previous errors
        binding.emailTextInput.error = null
        binding.passwordTextInput.error = null

        if (email.isEmpty()) {
            binding.emailTextInput.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailTextInput.error = "Invalid email address"
            isValid = false
        }

        if (pass.isEmpty()) {
            binding.passwordTextInput.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    // This function runs when the fragment view is destroyed.
    // Setting binding to null helps prevent memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}