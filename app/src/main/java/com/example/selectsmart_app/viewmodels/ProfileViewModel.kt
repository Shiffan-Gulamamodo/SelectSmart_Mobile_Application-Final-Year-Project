package com.example.selectsmart_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.Order
import com.example.selectsmart_app.models.OrderItem
import com.example.selectsmart_app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ProfileViewModel manages the user's profile data and order history.
// It loads the logged-in user's details from Firestore,
// updates profile information, and retrieves previous orders with their order items.
class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Private MutableLiveData used to store the user's profile.
    private val _userProfile = MutableLiveData<User?>()
    // Public LiveData observed by ProfileFragment.
    // The UI can read the profile data but cannot directly modify it.
    val userProfile: LiveData<User?> = _userProfile

    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Stores the Firestore real-time listener for orders.
    // This allows it to be removed later to prevent memory leaks.
    private var ordersListener: ListenerRegistration? = null

    // This function loads the current user's profile from the Users collection.
    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        // Launches a coroutine so Firestore can be accessed asynchronously.
        viewModelScope.launch {
            try {
                val document = db.collection("Users").document(userId).get().await()
                // If the document exists, convert it into a User object.
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    // Updates LiveData so the UI can display the user profile.
                    _userProfile.value = user
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            }
        }
    }

    // This function loads the user's order history.
    // It uses a real-time listener so the order history updates automatically
    // if the order data changes in Firestore.
    fun loadOrderHistory() {
        val userId = auth.currentUser?.uid ?: return

        // Removes any existing order listener before creating a new one.
        // This prevents multiple listeners running at the same time.
        ordersListener?.remove()

        // Listens to orders belonging to the current user,
        // ordered by the newest order date first.
        ordersListener = db.collection("Orders")
            .whereEqualTo("UserId", userId)
            .orderBy("OrderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                // If there is an error, try loading orders without orderBy.
                // This can help if Firestore requires an index for the query.
                if (e != null) {
                    fetchOrdersWithoutIndex(userId)
                    return@addSnapshotListener
                }

                // If order documents are returned, convert them into Order objects.
                snapshot?.let {
                    val orderList = it.toObjects(Order::class.java)
                    // Updates LiveData with the order list.
                    _orders.postValue(orderList)
                    // Loads the order items linked to each order.
                    fetchItemsForOrders(orderList)
                }
            }
    }

    // This function is a fallback for loading orders if the indexed query fails.
    // It loads orders by UserId only, then sorts them locally by order date.
    private fun fetchOrdersWithoutIndex(userId: String) {
        db.collection("Orders")
            .whereEqualTo("UserId", userId)
            .addSnapshotListener { snapshot, e ->
                // If loading still fails, store an error message.
                if (e != null) {
                    _error.postValue("Failed to load orders: ${e.message}")
                    return@addSnapshotListener
                }
                // Converts the Firestore documents into Order objects
                // and sorts them by newest order date first.
                snapshot?.let {
                    val orderList = it.toObjects(Order::class.java).sortedByDescending { it.orderDate }
                    // Updates LiveData with the sorted order list.
                    _orders.postValue(orderList)
                    // Loads the order items for each order.
                    fetchItemsForOrders(orderList)
                }
            }
    }

    // This function loads all order items for the current user.
    // It is more efficient because it fetches all items in one query
    // and then groups them by OrderId.
    private fun fetchItemsForOrders(orderList: List<Order>) {
        val userId = auth.currentUser?.uid ?: return
        // Launches a coroutine for Firestore loading.
        viewModelScope.launch {
            try {
                // Fetch all order items for this user at once (more efficient)
                val itemsSnapshot = db.collection("OrderItems")
                    .whereEqualTo("UserId", userId)
                    .get()
                    .await()

                val allItems = itemsSnapshot.toObjects(OrderItem::class.java)

                // Groups the order items by their OrderId.
                // This makes it easier to attach items to the correct order.
                val itemsByOrder = allItems.groupBy { it.orderId }
                
                // Assign items to their respective orders
                for (order in orderList) {
                    order.items = itemsByOrder[order.orderId] ?: emptyList()
                }
                
                // Post a new list reference to ensure LiveData triggers the observer
                _orders.postValue(ArrayList(orderList))
            } catch (e: Exception) {
                // If fetching all items at once fails,
                // use the safer but less efficient per-order fallback.
                fetchItemsIndividually(orderList)
            }
        }
    }

    // This fallback function loads order items separately for each order.
    // It is less efficient but useful if the grouped query fails.
    private suspend fun fetchItemsIndividually(orderList: List<Order>) {
        // Loops through each order.
        for (order in orderList) {
            try {
                // Fetches order items linked to the current order ID.
                val itemsSnapshot = db.collection("OrderItems")
                    .whereEqualTo("OrderId", order.orderId)
                    .get()
                    .await()
                // Converts the documents into OrderItem objects
                // and assigns them to the order.
                order.items = itemsSnapshot.toObjects(OrderItem::class.java)
            } catch (e: Exception) {
            // Error is ignored here so one failed order item query
            // does not stop the whole order history from loading.
            }
        }
        _orders.postValue(ArrayList(orderList))
    }

    // This function updates the user's profile details in Firestore.
    // It updates first name, last name and address details.
    fun updateProfile(firstName: String, lastName: String, address: String, postCode: String, city: String, country: String) {
        val userId = auth.currentUser?.uid ?: return
        // Creates a map for the nested UserAddress object in Firestore.
        val addressMap = hashMapOf(
            "AddressLine1" to address,
            "PostCode" to postCode,
            "City" to city,
            "Country" to country
        )
        // Creates a map containing all profile fields to update.
        val updates = hashMapOf<String, Any>(
            "UserFirstName" to firstName,
            "UserLastName" to lastName,
            "UserAddress" to addressMap
        )
        // Launches a coroutine to update Firestore asynchronously.
        viewModelScope.launch {
            try {
                // Updates the user's document in the Users collection.
                db.collection("Users").document(userId).update(updates).await()
                // Updates LiveData to show that the profile update was successful.
                _updateSuccess.value = true
                // Reloads the profile so the UI shows the latest saved data.
                loadUserProfile()
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            }
        }
    }

    // Function to update password
    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }
}
