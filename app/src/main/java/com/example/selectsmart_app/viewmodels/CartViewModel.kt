package com.example.selectsmart_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.Cart
import com.example.selectsmart_app.models.CartItem
import com.example.selectsmart_app.models.Product
import com.example.selectsmart_app.repositories.CartRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// CartViewModel manages shopping cart functionality in the SelectSmart app.
// It connects the cart UI screens to CartRepository, which handles Firestore operations.
// It also stores cart data using LiveData so the UI can update automatically.
class CartViewModel : ViewModel() {

    // Creates an instance of CartRepository.
    // This repository contains the main Firestore cart operations.
    private val cartRepository = CartRepository()

    // Private MutableLiveData used inside the ViewModel to store cart items.
    private val _cartItems = MutableLiveData<List<CartItem>>()

    // Public LiveData observed by the UI
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _subtotal = MutableLiveData<Double>()
    val subtotal: LiveData<Double> = _subtotal

    private val _shippingCost = MutableLiveData<Double>()
    val shippingCost: LiveData<Double> = _shippingCost

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    // Stores loading state so the UI can show/hide loading or disable buttons.
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _addToCartSuccess = MutableLiveData<Boolean>()
    val addToCartSuccess: LiveData<Boolean> = _addToCartSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentCart: Cart? = null
    // Stores already-loaded products to avoid repeatedly fetching the same product from Firestore.
    // This improves performance when cart items need product details.
    private val productCache = mutableMapOf<String, Product>()

    // This function loads the current user's cart and cart items.
    // It retrieves the cart from Firestore, loads all cart items,
    // then calculates subtotal, shipping and total price.
    fun loadCart() {
        viewModelScope.launch {
            // Sets loading state to true while cart data is being loaded.
            _isLoading.value = true
            try {
                // Retrieves the current user's cart from CartRepository.
                currentCart = cartRepository.getCart()
                // If a cart exists, load the cart items using the cart ID.
                currentCart?.let { cart ->
                    val items = cartRepository.getCartItems(cart.cartId)
                    _cartItems.value = items
                    calculateCosts(items)
                // If no cart exists, show an empty cart and calculate zero costs.
                } ?: run {
                    _cartItems.value = emptyList()
                    calculateCosts(emptyList())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load cart: ${e.message}"
                _cartItems.value = emptyList()
            } finally {
                // Stops the loading state after the operation finishes.
                _isLoading.value = false
            }
        }
    }

    // This function adds a product to the user's cart.
    // It first gets or creates the user's cart, then adds the selected product.
    fun addToCart(productId: String, price: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Gets the current user's cart from Firestore.
                val cart = cartRepository.getCart()
                // If the cart exists, add the selected product to it.
                if (cart != null && cart.cartId.isNotEmpty()) {
                    cartRepository.addToCart(cart.cartId, productId, price)
                    // Updates success LiveData so the UI can show a success message.
                    _addToCartSuccess.value = true
                } else {
                    _errorMessage.value = "Could not initialize cart. Please try again."
                    _addToCartSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add to basket: ${e.message}"
                _addToCartSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // This function resets the add-to-cart success and error states.
    // It prevents old success/error messages from appearing again.
    fun resetAddToCartStatus() {
        _addToCartSuccess.value = false
        _errorMessage.value = null
    }

    // This function updates the quantity of a product already in the cart.
    // After updating the quantity in Firestore, it reloads the cart
    // so the UI shows the latest data and recalculated total.
    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        viewModelScope.launch {
            try {
                // Updates the cart item quantity in Firestore.
                cartRepository.updateCartItemQuantity(cartItem.cartItemId, newQuantity)
                // Reloads the cart after the update.
                loadCart()
            } catch (e: Exception) {
                _errorMessage.value = "Update failed: ${e.message}"
            }
        }
    }

    // This function removes a product from the cart.
    // After removing the item from Firestore, it reloads the cart
    // to update the UI and totals.
    fun removeItem(cartItem: CartItem) {
        viewModelScope.launch {
            try {
                // Removes the selected cart item from Firestore.
                cartRepository.removeCartItem(cartItem.cartItemId)
                loadCart()
            } catch (e: Exception) {
                _errorMessage.value = "Remove failed: ${e.message}"
            }
        }
    }

    // This function calculates the cart subtotal, shipping cost and total price.
    private fun calculateCosts(items: List<CartItem>) {
        // Calculates subtotal by multiplying each item's price by its quantity.
        val sub = items.sumOf { it.price * it.quantity }
        // Updates subtotal LiveData.
        _subtotal.value = sub
        
        // Shipping is 4.99, but free for orders over 200.00
        val shipping = when {
            sub == 0.0 -> 0.0
            sub >= 200.0 -> 0.0
            else -> 4.99
        }
        // Updates shipping cost and final total price.
        _shippingCost.value = shipping
        _totalPrice.value = sub + shipping
    }

    // This function gets product details either from the local cache or Firestore.
    // It is useful when the cart only stores product IDs but the UI needs product details.
    fun getProduct(productId: String, callback: (Product?) -> Unit) {
        // If the product is already stored in the cache, return it immediately.
        // This avoids unnecessary Firestore reads.
        if (productCache.containsKey(productId)) {
            callback(productCache[productId])
            return
        }

        viewModelScope.launch {
            try {
                // Retrieves the product document from Firestore using the product ID.
                val doc = FirebaseFirestore.getInstance().collection("Products").document(productId).get().await()
                if (doc.exists()) {
                    val product = doc.toObject(Product::class.java)
                    if (product != null) {
                        // Saves the product in cache for future use.
                        productCache[productId] = product
                        // Returns the product through the callback.
                        callback(product)
                    } else {
                        // Returns null if conversion fails.
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
}