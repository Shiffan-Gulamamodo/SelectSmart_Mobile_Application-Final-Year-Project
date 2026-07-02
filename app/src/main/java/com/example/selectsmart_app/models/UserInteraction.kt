package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// This data class is used to store user interaction behaviour
data class UserInteraction(
    var interactionId: String = "",
    val userId: String = "",
    val prodId: String = "",
    val interactionType: String = "",
    @get:PropertyName("InteractionTime")
    @set:PropertyName("InteractionTime")
    var interactionTime: Timestamp? = null,
    @get:PropertyName("CategoryId")
    @set:PropertyName("CategoryId")
    var categoryId: String = ""
)