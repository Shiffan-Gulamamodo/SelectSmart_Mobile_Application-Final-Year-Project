package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// this data class is used to represent payment information for orders
data class Payment(
    @DocumentId
    @get:PropertyName("PaymentId") @set:PropertyName("PaymentId") var paymentId: String = "",
    
    @get:PropertyName("OrderId") @set:PropertyName("OrderId") var orderId: String = "",
    @get:PropertyName("UserId") @set:PropertyName("UserId") var userId: String = "",
    @get:PropertyName("Amount") @set:PropertyName("Amount") var amount: Double = 0.0,
    
    @get:PropertyName("PaymentDate") @set:PropertyName("PaymentDate") @ServerTimestamp var paymentDate: Timestamp? = null,
    
    @get:PropertyName("PaymentMethod") @set:PropertyName("PaymentMethod") var paymentMethod: String = "",
    @get:PropertyName("Status") @set:PropertyName("Status") var status: String = "",
    @get:PropertyName("TransactionId") @set:PropertyName("TransactionId") var transactionId: String = ""
)