package com.example.selectsmart_app.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.selectsmart_app.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot

// SearchViewModel manages the product searching, filtering and sorting logic.
// It stores all products, applies search/filter rules, and exposes the final filtered list to SearchFragment.
class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    // Tag used for Logcat debugging.
    private val TAG = "SearchViewModelDebug"

    // Stores all products loaded from Firestore.
    // This is kept private because only the ViewModel should modify the full product list.
    private val _allProducts = MutableLiveData<List<Product>>(emptyList())

    private val _filteredProducts = MutableLiveData<List<Product>>(emptyList())

    // Public LiveData observed by SearchFragment to display the filtered product results.
    val filteredProducts: LiveData<List<Product>> = _filteredProducts

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    private val _brands = MutableLiveData<List<String>>(emptyList())
    val brands: LiveData<List<String>> = _brands

    // Maps Firestore Brand document IDs to readable brand names.
    // Example: "abc123" -> "Samsung"
    private val brandMap = mutableMapOf<String, String>() // Brand ID -> Brand Name
    private val categoryMap = mutableMapOf<String, String>() // Category ID -> Category Name

    // Stores the currently selected category filter.
    var selectedCategory: String? = null
    var selectedBrand: String? = null
    var minPrice: Double? = null
    var maxPrice: Double? = null
    var sortBy: SortOrder = SortOrder.PRICE_LOW_HIGH

    // Enum used to represent the two available price sorting options.
    enum class SortOrder {
        PRICE_LOW_HIGH, PRICE_HIGH_LOW
    }

    // init runs automatically when the ViewModel is created.
    // It loads brands, categories and products from Firestore.
    init {
        Log.d(TAG, "ViewModel Initialized")
        loadBrands()
        loadCategories()
        loadProducts()
    }

    // This function loads brand data from Firestore.
    // It first checks the "Brand" collection, then falls back to "Brands" if needed.
    private fun loadBrands() {
        db.collection("Brand").get().addOnSuccessListener { snapshot ->
            // If the "Brand" collection is empty, try loading from "Brands".
            if (snapshot.isEmpty) {
                db.collection("Brands").get().addOnSuccessListener { s2 -> processBrands(s2.documents) }
                // If data exists, process the brand documents
            } else {
                processBrands(snapshot.documents)
            }
        }.addOnFailureListener {
            // If loading from "Brand" fails, try loading from "Brands".
            db.collection("Brands").get().addOnSuccessListener { s2 -> processBrands(s2.documents) }
        }
    }

    // This function processes Firestore brand documents.
    // It extracts the brand name and stores it in brandMap.
    private fun processBrands(documents: List<DocumentSnapshot>) {
        // Clears old brand data before adding new data.
        brandMap.clear()
        // Loops through each brand document.
        documents.forEach { doc ->
            // Gets the BrandName field from Firestore.
            val name = doc.getString("BrandName")?.trim() ?: ""
            // Stores the brand document ID and brand name if the name is not empty.
            if (name.isNotEmpty()) brandMap[doc.id] = name
        }
        // Updates the list of brand names shown in the filter spinner.
        updateBrandList()
        // Re-applies filters after brand data is loaded.
        applyFilters()
    }

    // This function loads category data from Firestore.
    // It first checks the "Category" collection, then falls back to "Categories" if needed.
    private fun loadCategories() {
        db.collection("Category").get().addOnSuccessListener { snapshot ->
            // If the "Category" collection is empty, try loading from "Categories".
            if (snapshot.isEmpty) {
                db.collection("Categories").get().addOnSuccessListener { s2 -> processCategories(s2.documents) }
            // If data exists, process the category documents.
            } else {
                processCategories(snapshot.documents)
            }
        }.addOnFailureListener {
            db.collection("Categories").get().addOnSuccessListener { s2 -> processCategories(s2.documents) }
        }
    }

    // This function processes Firestore category documents.
    // It extracts the category name and stores it in categoryMap.
    private fun processCategories(documents: List<DocumentSnapshot>) {
        // Clears old category data before adding new data.
        categoryMap.clear()
        // Loops through each category document.
        documents.forEach { doc ->
            // Gets the CategoryType field from Firestore.
            val name = doc.getString("CategoryType")?.trim() ?: ""
            // Stores the category document ID and category name if the name is not empty.
            if (name.isNotEmpty()) categoryMap[doc.id] = name
        }
        // Updates the list of categories shown in the filter spinner.
        updateCategoryList()
        // Re-applies filters after category data is loaded.
        applyFilters()
    }

    // This function loads all products from the Products collection in Firestore.
    private fun loadProducts() {
        db.collection("Products").get().addOnSuccessListener { snapshot ->
            val products = snapshot.documents.map { mapDocumentToProduct(it) }
            // Stores the full product list.
            _allProducts.value = products
            Log.d(TAG, "Loaded ${products.size} products")
            // Updates category and brand filter lists after products are loaded.
            updateCategoryList()
            updateBrandList()
            // Applies the current search/filter settings.
            applyFilters()
        }.addOnFailureListener { Log.e(TAG, "Error loading products", it) }
    }

    // This function manually maps a Firestore document into a Product object.
    private fun mapDocumentToProduct(doc: DocumentSnapshot): Product {
        return Product(
            ProdId = doc.id,
            ProdName = doc.getString("ProdName") ?: "",
            ProdBrand = doc.get("ProdBrand"),
            CategoryId = doc.get("CategoryId"),
            ProdPrice = parseDouble(doc.get("ProdPrice")),
            ProdDescription = doc.getString("ProdDescription") ?: "",
            ProdImage = doc.getString("ProdImage") ?: "",
            ProdRating = parseDouble(doc.get("ProdRating")),
            ProdStock = (doc.get("ProdStock") as? Number)?.toInt() ?: 0,
            ProdReleaseYear = (doc.get("ProdReleaseYear") as? Number)?.toInt() ?: 0,
            ProdSpecification = (doc.get("ProdSpecification") as? Map<String, Any>) ?: emptyMap()
        )
    }

    // This helper function safely converts Firestore values into Double.
    // It handles values stored as Number or String.
    private fun parseDouble(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    // This function updates the category list used by the filter spinner.
    private fun updateCategoryList() {
        // Starts with category names from categoryMap
        val names = categoryMap.values.filter { !isLikelyFirestoreId(it) }.toMutableSet()
        // Also checks product category values in case category names are stored directly in products.
        _allProducts.value?.forEach { p ->
            val catValue = p.CategoryId
            // Only add the category if it is a readable String and not a Firestore path.
            if (catValue is String && !catValue.contains("/")) {
                val name = catValue.trim()
                // Adds the category name if it is valid and not already stored.
                if (name.isNotEmpty() && !categoryMap.containsValue(name) &&
                    !categoryMap.containsKey(name) && !isLikelyFirestoreId(name)) {
                    names.add(name)
                }
            }
        }
        // Updates LiveData with sorted category names.
        _categories.value = names.filter { it.isNotBlank() }.sorted()
    }

    // This function updates the brand list used by the filter spinner.
    private fun updateBrandList() {
        // Starts with brand names from brandMap.
        val names = brandMap.values.filter { !isLikelyFirestoreId(it) }.toMutableSet()
        // Also checks product brand values in case brand names are stored directly in products.
        _allProducts.value?.forEach { p ->
            val brandValue = p.ProdBrand
            // Only add the brand if it is a readable String and not a Firestore path.
            if (brandValue is String && !brandValue.contains("/")) {
                val name = brandValue.trim()
                // Adds the brand name if it is valid and not already stored.
                if (name.isNotEmpty() && !brandMap.containsValue(name) &&
                    !brandMap.containsKey(name) && !isLikelyFirestoreId(name)) {
                    names.add(name)
                }
            }
        }
        // Updates LiveData with sorted brand names.
        _brands.value = names.filter { it.isNotBlank() }.sorted()
    }

    // This function checks if a String looks like a Firestore document ID.
    // Firestore IDs are usually long, contain no spaces, and are alphanumeric.
    private fun isLikelyFirestoreId(s: String): Boolean {
        return s.length >= 15 && !s.contains(" ") && s.all { it.isLetterOrDigit() }
    }

    // This helper function extracts a clean ID from different Firestore value types.
    // It supports normal Strings, Firestore paths, and DocumentReference values.
    private fun extractId(value: Any?): String? {
        return when (value) {
            // If it is a Firestore path like "Brand/abc123", take only the final ID.
            is String -> if (value.contains("/")) value.substringAfterLast("/").trim() else value.trim()
            // If it is a DocumentReference, return the document ID.
            is DocumentReference -> value.id
            // Otherwise, convert the value to String.
            else -> value?.toString()?.trim()
        }
    }
    // This function updates the current search query and applies search filtering.
    fun setSearchQuery(query: String) {
        // Gets the current query to prevent unnecessary filtering.
        val current = _searchQuery.value ?: ""
        // If the query has not changed, do nothing.
        if (query == current) return

        Log.d(TAG, "setSearchQuery: '$query'")

        // If the user types a search query, clear filters.
        // This keeps search mode separate from filter mode.
        if (query.isNotBlank()) {
            Log.d(TAG, "Clearing filters because search is active")
            selectedCategory = null
            selectedBrand = null
            minPrice = null
            maxPrice = null
        }

        // Updates the search query value.
        _searchQuery.value = query
        // Applies the search to the product list.
        applyFilters()
    }

    // Function to clear the search query and reset all filters
    fun clearSearch() {
        _searchQuery.value = ""
        selectedCategory = null
        selectedBrand = null
        minPrice = null
        maxPrice = null
        applyFilters()
    }

    // Function to set filters and apply filters
    fun setFilters(category: String?, brand: String?, min: Double?, max: Double?, sort: SortOrder) {
        Log.d(TAG, "setFilters called - Cat: $category, Brand: $brand, Min: $min, Max: $max")

        // Clears search query when filters are applied.
        // This means filtered browsing and search text are handled separately.
        _searchQuery.value = ""

        // Stores category only if it is not "All" or empty.
        selectedCategory = if (category == "All" || category.isNullOrBlank()) null else category.trim()
        selectedBrand = if (brand == "All" || brand.isNullOrBlank()) null else brand.trim()
        minPrice = min
        maxPrice = max
        sortBy = sort

        applyFilters()
    }

    // This function applies search, filters and sorting to the full product list.
    private fun applyFilters() {
        // Starts with all products loaded from Firestore.
        var products = _allProducts.value ?: emptyList()
        // Gets the current search query.
        val currentQuery = _searchQuery.value ?: ""

        Log.d(TAG, "applyFilters - Query: '$currentQuery', Cat: $selectedCategory, Brand: $selectedBrand, Products in: ${products.size}")

        // If the user has entered a search query, apply keyword search.
        if (currentQuery.isNotBlank()) {
            // Converts the search query to lowercase for case-insensitive matching.
            val q = currentQuery.lowercase().trim()
            // Filters products where the name or description contains the search query.
            products = products.filter { product ->
                val name = product.ProdName.lowercase()
                name.contains(q) || product.ProdDescription.lowercase().contains(q)
            }
            Log.d(TAG, "After Search Filter: ${products.size}")

            // Sorts search results by relevance.
            // Products where the name starts with the query appear first.
            // Then products where any word starts with the query appear next.
            products = products.sortedWith(compareByDescending<Product> {
                it.ProdName.lowercase().startsWith(q)
            }.thenByDescending {
                it.ProdName.lowercase().split(" ").any { word -> word.startsWith(q) }
            })
        // If there is no search query, apply category, brand, price and sort filters.
        } else {
            // Applies category filter if a category has been selected.
            selectedCategory?.let { categoryName ->
                // Finds the Firestore category ID linked to the readable category name.
                val categoryId = categoryMap.entries.find { it.value.equals(categoryName, ignoreCase = true) }?.key
                // Filters products where category matches by name or ID.
                products = products.filter { p ->
                    val prodCatValue = extractId(p.CategoryId)
                    prodCatValue?.equals(categoryName, ignoreCase = true) == true ||
                    (categoryId != null && prodCatValue == categoryId)
                }
                Log.d(TAG, "After Category Filter ($categoryName): ${products.size}")
            }

            // Applies brand filter if a brand has been selected.
            selectedBrand?.let { brandName ->
                // Finds the Firestore brand ID linked to the readable brand name.
                val brandId = brandMap.entries.find { it.value.equals(brandName, ignoreCase = true) }?.key
                // Filters products where brand matches by name or ID.
                products = products.filter { p ->
                    val prodBrandValue = extractId(p.ProdBrand)
                    prodBrandValue?.equals(brandName, ignoreCase = true) == true ||
                    (brandId != null && prodBrandValue == brandId)
                }
                Log.d(TAG, "After Brand Filter ($brandName): ${products.size}")
            }

            // Applies minimum price filter.
            minPrice?.let { min -> products = products.filter { it.ProdPrice >= min } }
            maxPrice?.let { max -> products = products.filter { it.ProdPrice <= max } }
            Log.d(TAG, "After Price Filter: ${products.size}")

            // Sorts products based on selected price sort order.
            products = when (sortBy) {
                SortOrder.PRICE_LOW_HIGH -> products.sortedBy { it.ProdPrice }
                SortOrder.PRICE_HIGH_LOW -> products.sortedByDescending { it.ProdPrice }
            }
        }

        // Updates the final filtered product list.
        // SearchFragment observes this and updates the RecyclerView.
        _filteredProducts.value = products
        Log.d(TAG, "applyFilters result: ${products.size} products")
    }
}
