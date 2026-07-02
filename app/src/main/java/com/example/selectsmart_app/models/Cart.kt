package com.example.selectsmart_app.models

import com.google.firebase.firestore.PropertyName

// data class is used to represent a shopping cart
data class Cart(
    // stores the unique cart ID from Firestore
    @get:PropertyName("CartId")
    @set:PropertyName("CartId")
    var cartId: String = "",

    @get:PropertyName("UserId")
    @set:PropertyName("UserId")
    var userId: String = ""
)