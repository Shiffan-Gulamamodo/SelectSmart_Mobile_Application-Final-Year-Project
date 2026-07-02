package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.selectsmart_app.adapters.OrderHistoryAdapter
import com.example.selectsmart_app.databinding.FragmentProfileBinding
import com.example.selectsmart_app.models.User
import com.example.selectsmart_app.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

// This fragment displays and manages the user's profile page.
// It shows the user's personal details, allows profile updates,
// displays order history, and handles user logout.
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ProfileViewModel is used to load and update user profile data
    // and retrieve the user's order history from Firestore.
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    // Adapter used to display the user's order history in the RecyclerView.
    private lateinit var orderAdapter: OrderHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Sets up the RecyclerView used to display previous orders.
        setupRecyclerView()
        // Sets up button click listeners such as back, save and logout.
        setupClickListeners()
        // Observes LiveData from ProfileViewModel.
        observeViewModel()

        // Loads the current user's profile details from Firestore.
        viewModel.loadUserProfile()
        // Loads the current user's order history from Firestore.
        viewModel.loadOrderHistory()
    }

    // This function sets up the RecyclerView for displaying order history.
    private fun setupRecyclerView() {
        // Creates the adapter with an empty list at first.
        orderAdapter = OrderHistoryAdapter(emptyList())
        // Sets the RecyclerView layout manager and adapter.
        binding.rvOrderHistory.apply {
            // Displays orders in a vertical list.
            layoutManager = LinearLayoutManager(requireContext())
            // Connects the adapter to the RecyclerView.
            adapter = orderAdapter
        }
    }

    // This function sets up button click events on the profile screen.
    private fun setupClickListeners() {
        // Back button returns the user to the previous screen.
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            updateProfile()
        }

        // Logout button signs the user out of Firebase Authentication
        // and navigates back to the login screen.
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val action = ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
            findNavController().navigate(action)
        }
    }

    // This function observes profile data, order history, update success and errors.
    // When the ViewModel data changes, the UI updates automatically.
    private fun observeViewModel() {
        // Observes the user profile data from ProfileViewModel.
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            // If user data exists, display it in the profile fields.
            user?.let { populateUserData(it) }
        }

        // Observes the user's order history.
        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            // If there are no orders, show the no-orders message.
            if (orders.isEmpty()) {
                binding.tvNoOrders.visibility = View.VISIBLE
                binding.rvOrderHistory.visibility = View.GONE
            // If orders exist, show the RecyclerView and update the adapter.
            } else {
                binding.tvNoOrders.visibility = View.GONE
                binding.rvOrderHistory.visibility = View.VISIBLE
                orderAdapter.updateOrders(orders)
            }
        }

        // Observes whether the profile update was successful.
        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
        }

        // Observes error messages from ProfileViewModel.
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This function fills the profile screen fields with user data from Firestore.
    private fun populateUserData(user: User) {
        // Displays the user's full name at the top of the profile page
        binding.tvUserFullName.text = "${user.userFirstName} ${user.userLastName}"
        // Fills the editable first name field.
        binding.etFirstName.setText(user.userFirstName)
        binding.etLastName.setText(user.userLastName)
        // Displays the user's email address.
        binding.etEmail.setText(user.userEmail)

        // If the user has saved address details, display them in the address fields.
        user.userAddress?.let { address ->
            binding.etAddressLine1.setText(address.addressLine1)
            binding.etPostcode.setText(address.postCode)
            binding.etCity.setText(address.city)
            binding.etCountry.setText(address.country)
        }
    }

    // This function collects updated profile information from the input fields
    // and sends it to ProfileViewModel to update Firestore.
    private fun updateProfile() {
        // Gets the updated first name.
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val addressLine1 = binding.etAddressLine1.text.toString().trim()
        val postCode = binding.etPostcode.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()

        // Validates that the name fields are not empty.
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Name fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Sends the updated profile information to ProfileViewModel.
        viewModel.updateProfile(firstName, lastName, addressLine1, postCode, city, country)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
