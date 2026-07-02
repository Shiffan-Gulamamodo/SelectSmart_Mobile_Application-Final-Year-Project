package com.example.selectsmart_app.repositories

import com.example.selectsmart_app.models.Cart
import com.example.selectsmart_app.models.CartItem
import com.example.selectsmart_app.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// This repository class is used to manage shopping cart operations
class CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // this function here is used to retrieve or create user cart
    suspend fun getCart(): Cart? {
        // gets currently authenticated user ID
        val userId = auth.currentUser?.uid ?: return null
        return try {
            // fetches cart document from Firestore
            val doc = db.collection("Cart").document(userId).get().await()
            // checks if cart already exists
            if (doc.exists()) {
                val cart = doc.toObject(Cart::class.java)
                // make sures that cart ID and user ID are assigned correctly
                cart?.apply {
                    if (cartId.isEmpty()) cartId = userId
                    if (this.userId.isEmpty()) this.userId = userId
                } ?: Cart(cartId = userId, userId = userId)
            } else {
                // creates a new cart for user if cart does not exist
                val cartData = mapOf(
                    "CartId" to userId,
                    "UserId" to userId
                )
                //stores new cart in database
                db.collection("Cart").document(userId).set(cartData).await()
                Cart(cartId = userId, userId = userId)
            }
        } catch (e: Exception) {
            Cart(cartId = userId, userId = userId)
        }
    }

    //Used to retrieve cart items
    suspend fun getCartItems(cartId: String): List<CartItem> {
        // returns empty list if cart ID is empty
        if (cartId.isEmpty()) return emptyList()
        return try {
            // fetches all cart items linked to cart ID
            db.collection("CartItems")
                .whereEqualTo("CartId", cartId)
                .get()
                .await()
                .toObjects(CartItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // This function is used to update cart item quantity
    suspend fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        try {
            // updates quantity field in Firestore
            db.collection("CartItems").document(cartItemId)
                .update("Quantity", newQuantity)
                .await()
        } catch (e: Exception) {}
    }

    // This function here is used to remove item from cart
    suspend fun removeCartItem(cartItemId: String) {
        try {
            // deletes cart item from Firestore
            db.collection("CartItems").document(cartItemId)
                .delete()
                .await()
        } catch (e: Exception) {}
    }

    // This function is used to add product to cart
    suspend fun addToCart(cartId: String, prodId: String, price: Double) {
        // checks if cart ID is empty
        if (cartId.isEmpty()) throw Exception("Cart ID is empty")
        
        try {
            //fetches product details from Firestore
            val productDoc = db.collection("Products").document(prodId).get().await()
            val prodName = productDoc.getString("ProdName") ?: "Unknown Product"
            val categoryId = productDoc.getString("CategoryId") ?: ""

            // checks if product already exists in cart
            val existing = db.collection("CartItems")
                .whereEqualTo("CartId", cartId)
                .whereEqualTo("ProdId", prodId)
                .get()
                .await()

            // if product exist updates quantity
            if (!existing.isEmpty) {
                val itemDoc = existing.documents.first()
                val currentQty = itemDoc.getLong("Quantity") ?: 0
                db.collection("CartItems").document(itemDoc.id)
                    .update("Quantity", currentQty + 1)
                    .await()
            } else {
                // create new cart item
                val itemData = mapOf(
                    "CartId" to cartId,
                    "ProdId" to prodId,
                    "ProdName" to prodName,
                    "Quantity" to 1,
                    "Price" to price,
                    "CategoryId" to categoryId
                )
                //stores new cart item in database
                db.collection("CartItems").add(itemData).await()
            }
        } catch (e: Exception) {
            throw e
        }
    }
}