package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId

// this data class is used to represent technology products
data class Product(
    @DocumentId
    val ProdId: String = "",
    val ProdName: String = "",
    val ProdBrand: Any? = null,
    val CategoryId: Any? = null,
    val ProdPrice: Double = 0.0,
    val ProdRating: Double = 0.0,
    val ProdDescription: String = "",
    val ProdImage: String = "",
    val ProdStock: Int = 0,
    val ProdReleaseYear: Int = 0,
    // this stores product technical specifications as key-value pairs
    val ProdSpecification: Map<String, Any> = emptyMap()
)
