package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId

// data class used to represent product categories
data class Category(
    @DocumentId
    val categoryId: String = "",
    val CategoryType: String = "",
    val CategoryImage: String = ""
)