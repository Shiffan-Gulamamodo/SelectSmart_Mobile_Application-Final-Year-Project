package com.example.selectsmart_app.repositories

import android.util.Log
import com.example.selectsmart_app.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// NotificationRepository manages notification-related Firestore operations.
// It retrieves user notifications in real time, marks notifications as read,
// and deletes notifications from Firestore.
class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // This function retrieves the current user's notifications in real time.
    // It returns a Flow, so the UI can continuously receive updated notification data.
    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        // Gets the currently authenticated user's ID.
        val userId = auth.currentUser?.uid
        // If no user is logged in, return an empty list and close the Flow.
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Creates a real-time Firestore listener for the user's notifications.
        // Whenever notifications are added, updated or deleted, this listener runs again.
        val subscription = firestore.collection("Notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                // If an error occurs, send an empty list to avoid crashing the app.
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Converts Firestore documents into Notification objects.
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                // Sorts notifications so the newest notification appears first.
                val sortedNotifications = notifications.sortedByDescending { it.timestamp }
                // sends updated notification list through the flow
                trySend(sortedNotifications)
            }

        // Removes the Firestore listener when the Flow is closed.
        // This helps prevent memory leaks and unnecessary background updates.
        awaitClose { subscription.remove() }
    }

    // This function marks a notification as read.
    // It updates the isRead field in the selected notification document.
    suspend fun markAsRead(notificationId: String) {
        try {
            // Updates the notification document in Firestore.
            firestore.collection("Notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking as read: ${e.message}")
        }
    }

    // this function here is used to delete notifications from firestore
    suspend fun deleteNotification(notificationId: String) {
        try {
            // this deletes notification document from Firestore
            firestore.collection("Notifications").document(notificationId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error deleting notification: ${e.message}")
        }
    }
}
