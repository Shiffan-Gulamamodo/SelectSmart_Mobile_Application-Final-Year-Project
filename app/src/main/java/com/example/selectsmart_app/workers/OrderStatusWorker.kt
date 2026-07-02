package com.example.selectsmart_app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.selectsmart_app.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// OrderStatusWorker is a background worker used to update order status automatically.
// It runs after an order has been placed and simulates the order moving
// from its original status to "Delivering" and then "Delivered".
class OrderStatusWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Firestore database instance used to update order status
    // and create order status notifications.
    private val firestore = FirebaseFirestore.getInstance()

    // doWork() is the main function that runs when WorkManager starts this worker.
    // Because this is a CoroutineWorker, suspend functions like delay() and await() can be used.
    override suspend fun doWork(): Result {
        // Gets the order ID passed from OrderRepository through WorkManager input data.
        // If the order ID is missing, the worker fails.
        val orderId = inputData.getString("orderId") ?: return Result.failure()

        // Gets the user ID passed from OrderRepository.
        // If the user ID is missing, the worker fails.
        val userId = inputData.getString("userId") ?: return Result.failure()

        return try {
            // Waits 5 seconds before updating the order status to "Delivering".
            // This simulates the order moving to the delivery stage.
            delay(5000)
            // Updates the order status in Firestore and creates a notification.
            updateStatus(orderId, userId, "Delivering", "Order Out for Delivery", "Your order #${orderId.takeLast(6)} is on its way!")

            // Waits another 5 seconds before updating the order status to "Delivered".
            delay(5000)
            // Updates the order status to delivered and creates another notification.
            updateStatus(orderId, userId, "Delivered", "Order Delivered", "Your order #${orderId.takeLast(6)} has been delivered. Enjoy!")

            // Returns success if both status updates complete successfully.
            Result.success()
        } catch (e: Exception) {
            Log.e("OrderStatusWorker", "Error updating status", e)
            // Retries the worker if an error occurs, such as a Firestore connection issue.
            Result.retry()
        }
    }

    // This helper function updates the order status in Firestore
    // and creates a notification for the user.
    private suspend fun updateStatus(orderId: String, userId: String, status: String, title: String, message: String) {
        // Updates the OrderStatus field inside the selected order document.
        firestore.collection("Orders").document(orderId)
            .update("OrderStatus", status).await()

        // Creates a new document reference inside the Notifications collection.
        val ref = firestore.collection("Notifications").document()

        // Creates a Notification object containing the order update message.
        val notification = Notification(
            notificationId = ref.id,
            userId = userId,
            title = title,
            message = message,
            timestamp = com.google.firebase.Timestamp.now(),
            isRead = false,
            orderId = orderId,
            type = "ORDER_STATUS"
        )
        // Saves the notification in Firestore.
        ref.set(notification).await()
    }
}
