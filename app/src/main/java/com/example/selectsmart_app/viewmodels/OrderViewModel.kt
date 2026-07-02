package com.example.selectsmart_app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// OrderViewModel manages order details and real-time order status tracking.
// It loads the selected order, delivery details, payment details,
// and updates the order progress shown on the order success/status screen.
class OrderViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Private MutableLiveData used to store the current order.
    private val _order = MutableLiveData<Order?>()
    // Public LiveData observed by the UI.
    // The UI can read the order data but cannot directly modify it.
    val order: LiveData<Order?> = _order

    private val _delivery = MutableLiveData<Delivery?>()
    val delivery: LiveData<Delivery?> = _delivery

    private val _payment = MutableLiveData<Payment?>()
    val payment: LiveData<Payment?> = _payment

    private val _orderStatus = MutableLiveData<String>()
    val orderStatus: LiveData<String> = _orderStatus

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress

    // Stores the Firestore real-time listener.
    // This is used so the listener can be removed when it is no longer needed.
    private var orderListener: ListenerRegistration? = null

    // Stores the currently loaded order ID.
    // This prevents the same order from being loaded again unnecessarily.
    private var currentOrderId: String? = null

    // This function fetches order details using the order ID.
    // It also starts a real-time Firestore listener so the UI updates
    // whenever the order status changes in the database.
    fun fetchOrderDetails(orderId: String) {
        // If the same order is already being tracked, do nothing.
        if (currentOrderId == orderId) return
        // Stores the current Order ID
        currentOrderId = orderId

        // Removes any previous listener before creating a new one.
        // This prevents multiple listeners running at the same time.
        orderListener?.remove()

        // Adds a real-time listener to the selected order document.
        // Whenever the document changes in Firestore, this listener runs again.
        orderListener = firestore.collection("Orders").document(orderId)
            .addSnapshotListener { snapshot, e ->
                // If there is an error listening to the document, log it and stop.
                if (e != null) {
                    Log.e("OrderViewModel", "Listen failed", e)
                    return@addSnapshotListener
                }

                // Converts the Firestore order document into an Order object.
                val ord = snapshot?.toObject(Order::class.java)
                // Updates the order LiveData so the UI can display the order details.
                _order.postValue(ord)

                // If the order exists, update status and load related data.
                ord?.let {
                    // Updates the order status text and progress value.
                    // The status itself is updated in Firestore by WorkManager.
                    updateStatusUI(it.orderStatus)

                    // Loads delivery and payment details only once.
                    // This avoids repeatedly fetching the same associated data.
                    if (_delivery.value == null) loadAssociatedData(it)
                }
            }
    }

    // This function updates the order status text and progress percentage.
    // The progress value is based on the current order status.
    private fun updateStatusUI(status: String) {
        // Updates the status text shown in the UI.
        _orderStatus.postValue(status)
        // Updates progress based on the current status.
        _progress.postValue(when (status) {
            "Delivered" -> 100
            "Delivering" -> 66
            "Processing" -> 33
            else -> 0
        })
    }

    // This function loads the delivery and payment documents linked to the order.
    // The order stores deliveryId and paymentId, which are used to retrieve
    // the associated records from Firestore.
    private fun loadAssociatedData(ord: Order) {
        // Launches a coroutine so Firestore loading runs asynchronously.
        viewModelScope.launch {
            try {
                // If the order has a delivery ID, load the matching delivery document.
                if (ord.deliveryId.isNotEmpty()) {
                    // Converts the delivery document into a Delivery object
                    // and updates the delivery LiveData.
                    val dDoc = firestore.collection("Delivery").document(ord.deliveryId).get().await()
                    _delivery.postValue(dDoc.toObject(Delivery::class.java))
                }
                // If the order has a payment ID, load the matching payment document.
                if (ord.paymentId.isNotEmpty()) {
                    val pDoc = firestore.collection("Payment").document(ord.paymentId).get().await()
                    _payment.postValue(pDoc.toObject(Payment::class.java))
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error loading delivery/payment", e)
            }
        }
    }

    // This function runs when the ViewModel is destroyed.
    // It removes the Firestore listener to prevent memory leaks
    // and unnecessary background updates.
    override fun onCleared() {
        super.onCleared()
        orderListener?.remove()
    }
}
