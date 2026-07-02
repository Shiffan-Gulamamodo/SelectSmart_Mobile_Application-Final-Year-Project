package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId

// data class used to represent product brand information
data class Brand(
    @DocumentId
    val BrandId: String = "",
    val BrandName: String = ""
)
