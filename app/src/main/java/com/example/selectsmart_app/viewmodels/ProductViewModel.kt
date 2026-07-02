package com.example.selectsmart_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selectsmart_app.models.Category
import com.example.selectsmart_app.models.Product
import com.example.selectsmart_app.repositories.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// VewModel class used to manage product data for the home and product list screens
class ProductViewModel : ViewModel() {

    // Creates an instance of ProductRepository.
    // The repository handles the actual Firestore product queries.
    private val repository = ProductRepository()

    // Private MutableLiveData for storing product categories.
    // It can only be changed inside this ViewModel.
    private val _categories = MutableLiveData<List<Category>>()

    // Public LiveData for categories.
    // The UI observes this but cannot directly modify it.
    val categories: LiveData<List<Category>> = _categories

    private val _featuredProducts = MutableLiveData<List<Product>>()
    val featuredProducts: LiveData<List<Product>> = _featuredProducts

    private val _smartPhones = MutableLiveData<List<Product>>()
    val smartPhones: LiveData<List<Product>> = _smartPhones

    private val _laptops = MutableLiveData<List<Product>>()
    val laptops: LiveData<List<Product>> = _laptops

    private val _consoles = MutableLiveData<List<Product>>()
    val consoles: LiveData<List<Product>> = _consoles

    private val _recentlyViewed = MutableLiveData<List<Product>>()
    val recentlyViewed: LiveData<List<Product>> = _recentlyViewed

    private val _recentSearchProducts = MutableLiveData<List<Product>>()
    val recentSearchProducts: LiveData<List<Product>> = _recentSearchProducts

    private val _purchaseRecommendations = MutableLiveData<List<Product>>()
    val purchaseRecommendations: LiveData<List<Product>> = _purchaseRecommendations

    private val _categoryProducts = MutableLiveData<List<Product>>()
    val categoryProducts: LiveData<List<Product>> = _categoryProducts

    // Load data for the home screen
    fun loadHomeData(userId: String) {
        // viewModelScope launches a coroutine linked to the ViewModel lifecycle.
        // It allows Firestore data to be loaded asynchronously without freezing the UI.
        viewModelScope.launch {
            // Load core data
            _categories.value = repository.getCategories()
            _featuredProducts.value = repository.getFeaturedProducts()
            
            // Standard category sections
            _smartPhones.value = repository.getProductsByCategory("SmartPhones", 6)
            _laptops.value = repository.getProductsByCategory("Laptops", 6)
            _consoles.value = repository.getProductsByCategory("Consoles", 6)

            // These three recommendation queries run in parallel using async.
            // This improves loading speed because the app does not wait
            // for one recommendation query to finish before starting the next.
            val purchaseDeferred = async { repository.getPurchaseRecommendations(userId) }
            val searchDeferred = async { repository.getSearchRecommendations(userId) }
            val viewDeferred = async { repository.getViewRecommendations(userId) }

            // Waits for the purchase recommendation query to finish
            // and updates the LiveData for purchase-based recommendations.
            _purchaseRecommendations.value = purchaseDeferred.await()
            _recentSearchProducts.value = searchDeferred.await()
            _recentlyViewed.value = viewDeferred.await()
        }
    }

    // Load data for a specific category
    fun loadAllProductsForCategory(categoryId: String) {
        viewModelScope.launch {
            // Passing null as the limit means all products in that category are loaded.
            _categoryProducts.value = repository.getProductsByCategory(categoryId, null)
        }
    }
}