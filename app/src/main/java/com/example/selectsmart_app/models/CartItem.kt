package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// The data class used to represent products stored inside the shopping cart
data class CartItem(
    @DocumentId
    var cartItemId: String = "",

    // Stores the cart ID linked to the cart item
    @get:PropertyName("CartId")
    @set:PropertyName("CartId")
    var cartId: String = "",

    @get:PropertyName("ProdId")
    @set:PropertyName("ProdId")
    var prodId: String = "",

    @get:PropertyName("ProdName")
    @set:PropertyName("ProdName")
    var prodName: String = "",

    @get:PropertyName("Quantity")
    @set:PropertyName("Quantity")
    var quantity: Int = 0,

    @get:PropertyName("Price")
    @set:PropertyName("Price")
    var price: Double = 0.0,

    // stores the category ID linked to the product
    @get:PropertyName("CategoryId")
    @set:PropertyName("CategoryId")
    var categoryId: String = ""
)