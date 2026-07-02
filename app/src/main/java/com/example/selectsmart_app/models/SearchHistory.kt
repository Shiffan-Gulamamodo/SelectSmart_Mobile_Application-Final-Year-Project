package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


// this data class is used to store user search history
data class SearchHistory(
    @get:PropertyName("searchId")
    @set:PropertyName("searchId")
    var searchId: String = "",

    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("searchQuery")
    @set:PropertyName("searchQuery")
    var searchQuery: String = "",

    @get:PropertyName("Timestamp")
    @set:PropertyName("Timestamp")
    var timestamp: Timestamp? = null
)