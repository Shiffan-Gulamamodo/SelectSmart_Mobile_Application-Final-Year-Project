package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// data class used to represent user notifications
data class Notification(
    var notificationId: String = "",

    // stores the user ID linked to the notification
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    var title: String = "",
    var message: String = "",

    // automatically stores the server timestamp when notification is created
    @ServerTimestamp
    var timestamp: Timestamp? = null,

    // indicates whether notification has been read by the user
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    // Stores linked order ID for order-related notifications
    var orderId: String? = null,
    var type: String = "ORDER_STATUS"
)