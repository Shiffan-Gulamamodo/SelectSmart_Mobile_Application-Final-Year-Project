package com.example.selectsmart_app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.CartItem
import com.example.selectsmart_app.models.Delivery
import com.example.selectsmart_app.models.Payment
import com.example.selectsmart_app.models.User
import com.example.selectsmart_app.repositories.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// CheckoutViewModel manages the checkout and order placement process.
// It connects the CheckoutFragment to OrderRepository and handles checkout status updates.
// AndroidViewModel is used instead of normal ViewModel because OrderRepository needs application context.
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Private MutableLiveData used inside the ViewModel to store checkout status.
    // It can represent loading, success, or error.
    private val _checkoutStatus = MutableLiveData<CheckoutState>()

    // Private MutableLiveData used to store the current user's profile.
    val checkoutStatus: LiveData<CheckoutState> = _checkoutStatus

    private val _userProfile = MutableLiveData<User?>()
    // Public LiveData observed by CheckoutFragment to auto-fill user details.
    val userProfile: LiveData<User?> = _userProfile

    // init runs automatically when the ViewModel is created.
    // It loads the current user's profile from Firestore.
    init {
        fetchUserProfile()
    }

    // This function retrieves the current user's profile from Firestore.
    // It is used to auto-fill checkout delivery fields such as name and address.
    private fun fetchUserProfile() {
        // Gets the current user's ID.
        // If no user is logged in, the function stops.
        val userId = auth.currentUser?.uid ?: return
        // Launches a coroutine so Firestore loading runs asynchronously.
        viewModelScope.launch {
            try {
                // Gets the user document from the Users collection.
                val document = db.collection("Users").document(userId).get().await()
                // If the document exists, convert it into a User object.
                if (document.exists()) {
                    _userProfile.value = document.toObject(User::class.java)
                }
            } catch (e: Exception) {
            // Profile loading errors are ignored here because checkout can still work
            // even if the profile cannot be auto-filled.
            }
        }
    }

    // This function places an order using the delivery details,
    // selected payment method, cart items and total price.
    fun placeOrder(
        fullName: String,
        phone: String,
        street: String,
        city: String,
        postcode: String,
        country: String,
        paymentMethod: String,
        cartItems: List<CartItem>,
        totalPrice: Double
    ) {
        // Checks that all delivery fields are filled in.
        // If any required delivery field is empty, return an error state.
        if (fullName.isBlank() || phone.isBlank() || street.isBlank() || city.isBlank() || postcode.isBlank() || country.isBlank()) {
            _checkoutStatus.value = CheckoutState.Error("Please fill all delivery details")
            return
        }

        // Checks that the cart is not empty before placing the order.
        if (cartItems.isEmpty()) {
            _checkoutStatus.value = CheckoutState.Error("Your cart is empty")
            return
        }

        // Launches a coroutine so the order placement process runs asynchronously.
        viewModelScope.launch {
            // Sets checkout status to loading so the UI can show "Processing..."
            // and disable the place order button.
            _checkoutStatus.value = CheckoutState.Loading

            // Creates a Delivery object using the delivery information entered by the user.
            val delivery = Delivery(
                fullName = fullName,
                phoneNumber = phone,
                streetName = street,
                city = city,
                postCode = postcode,
                country = country
            )

            // Creates a Payment object using the selected payment method and total price.
            val payment = Payment(
                paymentMethod = paymentMethod,
                amount = totalPrice
            )

            // Calls OrderRepository to save the order, delivery, payment,
            // order items and notification in Firestore.
            val result = orderRepository.placeOrder(delivery, payment, cartItems, totalPrice)

            // If the order is placed successfully, update checkout status with the order ID.
            result.onSuccess { orderId ->
                _checkoutStatus.value = CheckoutState.Success(orderId)
            }.onFailure { exception ->
                _checkoutStatus.value = CheckoutState.Error(exception.message ?: "Failed to place order")
            }
        }
    }

    // Sealed class representing the different states of the checkout process.
    // This makes it easier for CheckoutFragment to handle loading, success and error states.
    sealed class CheckoutState {
        // Represents checkout currently being processed.
        object Loading : CheckoutState()
        // Represents successful checkout and stores the created order ID.
        data class Success(val orderId: String) : CheckoutState()
        // Represents checkout failure and stores the error message.
        data class Error(val message: String) : CheckoutState()
    }
}