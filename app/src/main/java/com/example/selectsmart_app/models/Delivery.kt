package com.example.selectsmart_app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// data class for used to represent delivery information for orders
data class Delivery(
    @DocumentId
    var deliveryId: String = "",

    // stores the user ID linked to the delivery
    @get:PropertyName("UserId")
    @set:PropertyName("UserId")
    var userId: String = "",

    @get:PropertyName("FullName")
    @set:PropertyName("FullName")
    var fullName: String = "",

    @get:PropertyName("PhoneNumber")
    @set:PropertyName("PhoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("StreetName")
    @set:PropertyName("StreetName")
    var streetName: String = "",

    @get:PropertyName("City")
    @set:PropertyName("City")
    var city: String = "",

    @get:PropertyName("Postcode")
    @set:PropertyName("Postcode")
    var postCode: String = "",

    @get:PropertyName("Country")
    @set:PropertyName("Country")
    var country: String = ""
)
