package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentCheckoutBinding
import com.example.selectsmart_app.viewmodels.CartViewModel
import com.example.selectsmart_app.viewmodels.CheckoutViewModel
import java.util.Calendar
import java.util.Locale

// This fragment manages the checkout screen.
// It collects delivery and payment details, validates the input,
// displays the cart total, and sends the order details to CheckoutViewModel.
class CheckoutFragment : Fragment() {

    // ViewBinding instance for checkout layout
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    // Shared CartViewModel used to access cart items, subtotal, shipping and total price.
    // activityViewModels means it shares the same cart data with other fragments in the activity.
    private val cartViewModel: CartViewModel by activityViewModels()
    // CheckoutViewModel used for order processing
    private val checkoutViewModel: CheckoutViewModel by viewModels()

    // Creates fragment view using ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after fragment view has been created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup button listeners
        setupListeners()
        // Observe ViewModel LiveData
        observeViewModels()
    }

    // Function used to setup button click events
    private fun setupListeners() {
        // Navigate back to previous screen
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Handle place order button click
        binding.btnPlaceOrder.setOnClickListener {
            // Validate user inputs before placing order
            if (validateInputs()) {
                // Retrieve delivery information
                val fullName = binding.etFullName.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()
                val street = binding.etStreet.text.toString().trim()
                val city = binding.etCity.text.toString().trim()
                val postcode = binding.etPostcode.text.toString().trim()
                val country = binding.etCountry.text.toString().trim()
                
                // Get selected payment method
                val selectedPaymentId = binding.rgPayment.checkedRadioButtonId
                // If debit card is selected, use "Debit Card".
                // Otherwise, use "Credit Card".
                val paymentMethod = if (selectedPaymentId == R.id.rbDebitCard) "Debit Card" else "Credit Card"

                // Retrieve current cart items and total price
                val items = cartViewModel.cartItems.value ?: emptyList()
                val total = cartViewModel.totalPrice.value ?: 0.0

                // Sends all checkout details to CheckoutViewModel.
                // CheckoutViewModel will process and save the order.
                checkoutViewModel.placeOrder(
                    fullName, phone, street, city, postcode, country,
                    paymentMethod, items, total
                )
            }
        }
    }

    // This function validates all checkout input fields.
    // It checks delivery details, card details, expiry date and CVV.
    private fun validateInputs(): Boolean {
        // Delivery Validation
        if (binding.etFullName.text.isNullOrBlank()) {
            binding.etFullName.error = "Required"
            return false
        }
        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.error = "Required"
            return false
        }
        if (binding.etStreet.text.isNullOrBlank()) {
            binding.etStreet.error = "Required"
            return false
        }
        if (binding.etCity.text.isNullOrBlank()) {
            binding.etCity.error = "Required"
            return false
        }
        if (binding.etPostcode.text.isNullOrBlank()) {
            binding.etPostcode.error = "Required"
            return false
        }
        if (binding.etCountry.text.isNullOrBlank()) {
            binding.etCountry.error = "Required"
            return false
        }

        // Card Validation
        if (binding.etCardName.text.isNullOrBlank()) {
            binding.etCardName.error = "Required"
            return false
        }

        // Removes spaces from the card number before validation.
        val cardNumber = binding.etCardNumber.text.toString().replace(" ", "")
        // Checks whether the card number is at least 16 digits.
        if (cardNumber.length < 16) {
            binding.etCardNumber.error = "Invalid Card Number"
            return false
        }

        // Gets the expiry date entered by the user.
        val expiry = binding.etExpiry.text.toString().trim()
        // Checks if expiry date is empty.
        if (expiry.isEmpty()) {
            binding.etExpiry.error = "Required"
            return false
        }

        // Regex checks that the expiry date is in MM/YY format.
        // Example: 12/25
        val regex = Regex("^(0[1-9]|1[0-2])/([0-9]{2})$")
        if (!expiry.matches(regex)) {
            binding.etExpiry.error = "Use format MM/YY (e.g. 12/25)"
            return false
        }

        // Check if date is valid (between current year and 20 years in the future)
        // Splits the expiry date into month and year.
        val parts = expiry.split("/")
        // Converts the month from String to Int.
        val month = parts[0].toInt()
        // Converts YY format into full YYYY format.
        val year = 2000 + parts[1].toInt()

        // Gets the current date.
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        // Calendar.MONTH starts from 0, so +1 is needed.
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        // Validate whether card has expired
        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            binding.etExpiry.error = "Card has expired"
            return false
        }

        // Prevents unrealistic expiry dates, such as more than 20 years in the future.
        if (year > currentYear + 20) {
            binding.etExpiry.error = "Invalid year"
            return false
        }

        // Gets the CVV value.
        val cvv = binding.etCVV.text.toString()
        // Checks whether the CVV is at least 3 digits.
        if (cvv.length < 3) {
            binding.etCVV.error = "Invalid CVV"
            return false
        }

        // If all validation checks pass, return true.
        return true
    }

    // This function observes LiveData from CheckoutViewModel and CartViewModel.
    // It updates the checkout screen when user profile, cart totals or checkout status changes.
    private fun observeViewModels() {
        // Observes user profile information from CheckoutViewModel.
        // This is used to automatically fill in some delivery fields.
        checkoutViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Fills the full name field using the user's first and last name.
                binding.etFullName.setText("${it.userFirstName} ${it.userLastName}")

                // If the user has an address saved, fill in address fields.
                it.userAddress?.let { addr ->
                    binding.etStreet.setText(addr.addressLine1)
                    binding.etCity.setText(addr.city)
                    binding.etPostcode.setText(addr.postCode)
                    binding.etCountry.setText(addr.country)
                }
            }
        }

        // Observes subtotal and displays it in currency format.
        cartViewModel.subtotal.observe(viewLifecycleOwner) { subtotal ->
            binding.tvSubtotal.text = String.format(Locale.UK, "£%.2f", subtotal)
        }

        // Observes shipping cost and displays either "Free" or the shipping price.
        cartViewModel.shippingCost.observe(viewLifecycleOwner) { shipping ->
            binding.tvShipping.text = if (shipping == 0.0) "Free" else String.format(Locale.UK, "£%.2f", shipping)
        }

        // Observes total price and displays it in currency format.
        cartViewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.tvTotal.text = String.format(Locale.UK, "£%.2f", total)
        }

        // Observes checkout status from CheckoutViewModel.
        // This controls loading, success and error states.
        checkoutViewModel.checkoutStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                // Shows loading state while the order is being processed.
                is CheckoutViewModel.CheckoutState.Loading -> {
                    binding.btnPlaceOrder.isEnabled = false
                    binding.btnPlaceOrder.text = "Processing..."
                }
                // If order is successful, navigate to the order success screen.
                is CheckoutViewModel.CheckoutState.Success -> {
                    val action = CheckoutFragmentDirections.actionCheckoutFragmentToOrderSuccessFragment(state.orderId)
                    findNavController().navigate(action)
                }
                // If there is an error, re-enable the button and show the error message.
                is CheckoutViewModel.CheckoutState.Error -> {
                    binding.btnPlaceOrder.isEnabled = true
                    binding.btnPlaceOrder.text = "Place Order"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Clears binding reference when fragment view is destroyed
    // Setting binding to null helps prevent memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
