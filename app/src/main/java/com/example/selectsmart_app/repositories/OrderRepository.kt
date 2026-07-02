package com.example.selectsmart_app.repositories

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.selectsmart_app.models.*
import com.example.selectsmart_app.workers.OrderStatusWorker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// OrderRepository handles order-related Firestore operations.
// It is responsible for placing orders, saving delivery/payment/order data,
// creating notifications, clearing the cart, and starting the order status worker.
class OrderRepository(private val context: Context) {
    // Firestore database instance used to store order-related data.
    private val firestore = FirebaseFirestore.getInstance()
    // FirebaseAuth instance used to get the currently logged-in user.
    private val auth = FirebaseAuth.getInstance()

    // function used to place customer orders
    suspend fun placeOrder(
        delivery: Delivery,
        payment: Payment,
        cartItems: List<CartItem>,
        totalPrice: Double
    ): Result<String> {
        // Gets the current user's ID.
        // If no user is logged in, the order cannot be placed.
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            // Creates a Firestore batch operation.
            // A batch allows multiple database writes to be committed together.
            val batch = firestore.batch()

            // Creates a new document reference for the Delivery collection.
            val deliveryRef = firestore.collection("Delivery").document()
            // Creates delivery data to store in Firestore.
            val deliveryData = hashMapOf(
                "DeliveryId" to deliveryRef.id,
                "UserId" to userId,
                "FullName" to delivery.fullName,
                "PhoneNumber" to delivery.phoneNumber,
                "StreetName" to delivery.streetName,
                "City" to delivery.city,
                "Postcode" to delivery.postCode,
                "Country" to delivery.country
            )
            // Adds the delivery record to the batch.
            batch.set(deliveryRef, deliveryData)

            // Creates a new document reference for the Payment collection
            val paymentRef = firestore.collection("Payment").document()

            // Creates order data to store in Firestore.
            val orderRef = firestore.collection("Orders").document()
            val orderData = hashMapOf(
                "OrderId" to orderRef.id,
                "UserId" to userId,
                "TotalPrice" to totalPrice,
                "OrderStatus" to "Pending",
                "DeliveryId" to deliveryRef.id,
                "PaymentId" to paymentRef.id,
                "OrderDate" to Timestamp.now()
            )
            batch.set(orderRef, orderData)

            // Loops through each cart item and creates an OrderItems record for it.
            cartItems.forEach { cartItem ->
                // Creates a new document reference for each order item.
                val orderItemRef = firestore.collection("OrderItems").document()
                // Creates order item data using the cart item information.
                val orderItemData = hashMapOf(
                    "OrderItemId" to orderItemRef.id,
                    "OrderId" to orderRef.id,
                    "UserId" to userId,
                    "ProdId" to cartItem.prodId,
                    "ProdName" to cartItem.prodName,
                    "ProdPrice" to cartItem.price,
                    "Quantity" to cartItem.quantity,
                    "CategoryId" to cartItem.categoryId
                )
                batch.set(orderItemRef, orderItemData)
            }

            //Create Payment records to store in Firestore
            val paymentData = hashMapOf(
                "PaymentId" to paymentRef.id,
                "OrderId" to orderRef.id,
                "UserId" to userId,
                "Amount" to totalPrice,
                "Status" to "Successful",
                "PaymentMethod" to payment.paymentMethod,
                "TransactionId" to "TXN_${System.currentTimeMillis() / 1000}",
                "PaymentDate" to Timestamp.now()
            )
            batch.set(paymentRef, paymentData)

            // Gets the current user's document reference.
            val userRef = firestore.collection("Users").document(userId)
            // Updates the user's saved address using the delivery details entered at checkout.
            val addressUpdate = hashMapOf<String, Any>(
                "UserAddress" to hashMapOf(
                    "AddressLine1" to delivery.streetName,
                    "City" to delivery.city,
                    "Country" to delivery.country,
                    "PostCode" to delivery.postCode
                )
            )
            batch.update(userRef, addressUpdate)

            // Creates a new notification document reference.
            val notificationRef = firestore.collection("Notifications").document()
            // This tells the user their order was placed successfully.
            val notification = Notification(
                notificationId = notificationRef.id,
                userId = userId,
                title = "Order Placed Successfully",
                message = "Your order has been received. Order ID: ${orderRef.id.takeLast(6).uppercase()}",
                timestamp = Timestamp.now(),
                isRead = false,
                orderId = orderRef.id,
                type = "ORDER_STATUS"
            )
            batch.set(notificationRef, notification)

            // Retrieves all cart items belonging to the user/cart.
            val cartItemsQuery = firestore.collection("CartItems")
                .whereEqualTo("CartId", userId)
                .get()
                .await()

            // Deletes cart items after successful order placement
            cartItemsQuery.documents.forEach { itemDoc ->
                batch.delete(itemDoc.reference)
            }

            // Commit Firestore batch operation all together
            batch.commit().await()

            // Start background worker for order status update later
            enqueueStatusWorker(orderRef.id, userId)

            // Returns the created order ID if everything is successful.
            Result.success(orderRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // This function starts the background worker that updates order status.
    private fun enqueueStatusWorker(orderId: String, userId: String) {
        // Creates input data that will be passed to OrderStatusWorker.
        val data = Data.Builder()
            .putString("orderId", orderId)
            .putString("userId", userId)
            .build()

        // Creates a one-time WorkManager request for OrderStatusWorker.
        val workRequest = OneTimeWorkRequestBuilder<OrderStatusWorker>()
            .setInputData(data)
            .build()

        // Adds the worker task to WorkManager.
        // WorkManager will run the task in the background.
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}