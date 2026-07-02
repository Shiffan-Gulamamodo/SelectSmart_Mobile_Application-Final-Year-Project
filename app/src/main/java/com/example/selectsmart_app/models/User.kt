package com.example.selectsmart_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// this data class is used to represent application users
data class User(
    @get:PropertyName("UserId") @set:PropertyName("UserId") var userId: String = "",
    @get:PropertyName("UserEmail") @set:PropertyName("UserEmail") var userEmail: String = "",
    @get:PropertyName("UserFirstName") @set:PropertyName("UserFirstName") var userFirstName: String = "",
    @get:PropertyName("UserLastName") @set:PropertyName("UserLastName") var userLastName: String = "",
    @get:PropertyName("UserAddress") @set:PropertyName("UserAddress") var userAddress: UserAddress? = null,
    @get:PropertyName("UserCreatedAt") @set:PropertyName("UserCreatedAt") @ServerTimestamp var userCreatedAt: Timestamp? = null
)

// this data class is used to represent user address details
data class UserAddress(
    @get:PropertyName("AddressLine1") @set:PropertyName("AddressLine1") var addressLine1: String = "",
    @get:PropertyName("City") @set:PropertyName("City") var city: String = "",
    @get:PropertyName("Country") @set:PropertyName("Country") var country: String = "",
    @get:PropertyName("PostCode") @set:PropertyName("PostCode") var postCode: String = ""
)
