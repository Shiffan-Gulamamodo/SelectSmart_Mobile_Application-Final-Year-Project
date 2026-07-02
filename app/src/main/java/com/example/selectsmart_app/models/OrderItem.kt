package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Th data class used to represent individual products within an order
data class OrderItem(
    @DocumentId
    var orderItemId: String = "",

    @get:PropertyName("OrderId")
    @set:PropertyName("OrderId")
    var orderId: String = "",

    @get:PropertyName("UserId")
    @set:PropertyName("UserId")
    var userId: String = "",

    @get:PropertyName("ProdId")
    @set:PropertyName("ProdId")
    var prodId: String = "",

    @get:PropertyName("ProdName")
    @set:PropertyName("ProdName")
    var prodName: String = "",

    @get:PropertyName("ProdPrice")
    @set:PropertyName("ProdPrice")
    var prodPrice: Double = 0.0,

    @get:PropertyName("Quantity")
    @set:PropertyName("Quantity")
    var quantity: Int = 0,

    @get:PropertyName("CategoryId")
    @set:PropertyName("CategoryId")
    var categoryId: String = ""
)