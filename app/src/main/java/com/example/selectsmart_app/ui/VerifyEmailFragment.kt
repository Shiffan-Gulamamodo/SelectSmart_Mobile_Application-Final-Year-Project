package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.selectsmart_app.databinding.FragmentVerifyEmailBinding
import com.example.selectsmart_app.viewmodels.AuthViewModel

// Fragment class used to show password reset email verification screen
class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val args: VerifyEmailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.btnBack.setOnClickListener {
            authViewModel.resetState()
            findNavController().navigateUp()
        }

        binding.txtresend.setOnClickListener {
            authViewModel.sendPasswordResetEmail(args.email)
        }
    }

    // Function used to observe reset email status
    private fun setupObservers() {
        authViewModel.resetEmailResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    Toast.makeText(requireContext(), "Verification email resent", Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
