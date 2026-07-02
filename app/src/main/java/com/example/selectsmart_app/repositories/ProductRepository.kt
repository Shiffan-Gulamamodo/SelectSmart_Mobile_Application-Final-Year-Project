package com.example.selectsmart_app.repositories

import android.util.Log
import com.example.selectsmart_app.models.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// ProductRepository handles all product-related Firestore operations.
// It retrieves categories, products, featured products,
// and recommendation data used on the Home screen.
class ProductRepository {

    // Creates an instance of the Firestore database.
    // This allows the repository to read data from Firestore collections.
    private val db = FirebaseFirestore.getInstance()

    // functions used to retrieve all product categories
    suspend fun getCategories(): List<Category> {
        return try {
            //fetches all the categories from the database and converts them into objects
            db.collection("Category").get().await().toObjects(Category::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // this function is used to manually map Firestore document data into Product model
    private fun mapDocumentToProduct(doc: DocumentSnapshot): Product {
        return Product(
            // Assigns Firestore document ID as product ID
            ProdId = doc.id,
            ProdName = doc.getString("ProdName") ?: "",
            // retrieves product brand, handling both reference and string values
            ProdBrand = when (val brand = doc.get("ProdBrand")) {
                is DocumentReference -> brand.path
                else -> brand?.toString() ?: ""
            },
            CategoryId = doc.getString("CategoryId")?.trim() ?: "",
            ProdPrice = (doc.get("ProdPrice") as? Number)?.toDouble() ?: 0.0,
            ProdDescription = doc.getString("ProdDescription") ?: "",
            ProdImage = doc.getString("ProdImage") ?: "",
            ProdRating = (doc.get("ProdRating") as? Number)?.toDouble() ?: 0.0,
            ProdStock = (doc.get("ProdStock") as? Number)?.toInt() ?: 0,
            ProdReleaseYear = (doc.get("ProdReleaseYear") as? Number)?.toInt() ?: 0,
            ProdSpecification = (doc.get("ProdSpecification") as? Map<String, Any>) ?: emptyMap()
        )
    }

    // this is used to retrieve products based on category
    suspend fun getProductsByCategory(categoryId: String, limit: Long? = null): List<Product> {
        // Removes extra spaces from category ID
        val trimmedCategoryId = categoryId.trim()
        // Returns empty list if category ID is empty
        if (trimmedCategoryId.isEmpty()) return emptyList()
        return try {
            // Query products matching selected category
            var query: Query = db.collection("Products")
                .whereEqualTo("CategoryId", trimmedCategoryId)
            // Applys limit if provided
            if (limit != null) {
                query = query.limit(limit)
            }
            // fetches products and map documents into Product objects
            val snapshot = query.get().await()
            snapshot.documents.map { mapDocumentToProduct(it) }
        } catch (e: Exception) {
            Log.e("ProductRepo", "Error fetching products by category: $trimmedCategoryId", e)
            emptyList()
        }
    }

    //used to retrieve featured products
    suspend fun getFeaturedProducts(): List<Product> {
        return try {
            //retrieves featured products from the database the first five products
            val snapshot = db.collection("Products").limit(5).get().await()
            snapshot.documents.map { mapDocumentToProduct(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // This function recommends products based on the user's purchase history.
    // It checks what categories the user has previously ordered from,
    // then recommends more products from those categories.
    suspend fun getPurchaseRecommendations(userId: String): List<Product> {
        // If there is no user ID, recommendations cannot be generated.
        if (userId.isEmpty()) return emptyList()
        return try {
            // fetches order items previously purchased by user
            val snapshot = db.collection("OrderItems")
                .whereEqualTo("UserId", userId)
                .limit(20)
                .get()
                .await()
            
            val purchases = snapshot.toObjects(OrderItem::class.java)
            // Extract unique category IDs from previous purchases
            val categoryIds = purchases.map { it.categoryId.trim() }.filter { it.isNotEmpty() }.distinct()

            // If the user has no purchase categories, return no recommendations.
            if (categoryIds.isEmpty()) return emptyList()
            // Stores the recommended products before returning them.
            val recommended = mutableListOf<Product>()
            // recommends products from up to 3 previously purchased categories
            for (catId in categoryIds.take(3)) {
                recommended.addAll(getProductsByCategory(catId, 6))
            }
            // removes duplicates and return a maximum of 10 recommendation
            recommended.distinctBy { it.ProdId }.shuffled().take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // This function recommends products based on the user's search history.
    // It looks at recent search keywords and matches them with product names.y
    suspend fun getSearchRecommendations(userId: String): List<Product> {
        if (userId.isEmpty()) return emptyList()
        return try {
            // fetches recent user search history for current user
            val snapshot = db.collection("SearchHistory")
                .whereEqualTo("userId", userId)
                .limit(10)
                .get()
                .await()

            // Sorts searches by most recent first
            val searches = snapshot.toObjects(SearchHistory::class.java)
                .sortedByDescending { it.timestamp }
            // Stores the products recommended from search history.
            val recommended = mutableListOf<Product>()

            // Only generate recommendations if search history exists.
            if (searches.isNotEmpty()) {
                // Retrieves up to 100 products from Firestore.
                // These products are later checked against the user's search terms.
                val allProducts = db.collection("Products").limit(100).get().await()
                    .documents.map { mapDocumentToProduct(it) }

                // fetches the latest 3 search queries
                val recentQueries = searches.take(3).map { it.searchQuery.lowercase() }
                // Matches search keywords against product names
                for (query in recentQueries) {
                    if (query.isNotEmpty()) {
                        // Removes duplicate products and limit result
                        recommended.addAll(allProducts.filter { it.ProdName.lowercase().contains(query) })
                    }
                }
            }

            recommended.distinctBy { it.ProdId }.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // This function retrieves recently viewed products for the current user.
    // It uses the UserInteractions collection to find products the user has viewed.
    suspend fun getViewRecommendations(userId: String): List<Product> {
        if (userId.isEmpty()) return emptyList()
        return try {
            // fetches viewed product interactions for current user
            val snapshot = db.collection("UserInteractions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("interactionType", "view")
                .limit(20)
                .get()
                .await()

            // Sorts viewed products by most recent first
            val views = snapshot.toObjects(UserInteraction::class.java)
                .sortedByDescending { it.interactionTime }
            
            val viewedIds = views.map { it.prodId }.filter { it.isNotEmpty() }.distinct().take(10)
            if (viewedIds.isEmpty()) return emptyList()

            // Fetch the actual products from history
            val productsSnapshot = db.collection("Products")
                .whereIn(FieldPath.documentId(), viewedIds)
                .get()
                .await()
            
            val products = productsSnapshot.documents.map { mapDocumentToProduct(it) }
            
            // Returns them in the exact order they were viewed
            viewedIds.mapNotNull { id -> products.find { it.ProdId == id } }
        } catch (e: Exception) {
            Log.e("ProductRepo", "Error in recently viewed", e)
            emptyList()
        }
    }
}