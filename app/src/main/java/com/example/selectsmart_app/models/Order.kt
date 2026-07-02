package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// data class used to represent customer orders
data class Order(
    @DocumentId
    @get:PropertyName("OrderId")
    @set:PropertyName("OrderId")
    var orderId: String = "",
    
    @get:PropertyName("UserId") @set:PropertyName("UserId") var userId: String = "",
    @get:PropertyName("TotalPrice") @set:PropertyName("TotalPrice") var totalPrice: Double = 0.0,

    // automatically stores the order creation timestamp
    @get:PropertyName("OrderDate") @set:PropertyName("OrderDate") @ServerTimestamp var orderDate: Timestamp? = null,

    // stores the current order status
    @get:PropertyName("OrderStatus") @set:PropertyName("OrderStatus") var orderStatus: String = "Pending",
    @get:PropertyName("PaymentId") @set:PropertyName("PaymentId") var paymentId: String = "",
    @get:PropertyName("DeliveryId") @set:PropertyName("DeliveryId") var deliveryId: String = "",

    // stores order items locally but excludes them from Firestore mapping
    @get:Exclude @set:Exclude var items: List<OrderItem> = emptyList()
) {
    // This is used for manual mapping when items are stored as Firestore field
    @get:PropertyName("Items") @set:PropertyName("Items")
    var firestoreItems: List<OrderItem>? = null
        set(value) {
            field = value
            // This update local item list when Firestore items exist
            if (value != null) {
                items = value
            }
        }
}
