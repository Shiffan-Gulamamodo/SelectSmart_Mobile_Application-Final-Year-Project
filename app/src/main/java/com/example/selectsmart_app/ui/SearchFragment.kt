package com.example.selectsmart_app.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.selectsmart_app.R
import com.example.selectsmart_app.adapters.ProductListAdapter
import com.example.selectsmart_app.databinding.FragmentSearchBinding
import com.example.selectsmart_app.models.Product
import com.example.selectsmart_app.models.SearchHistory
import com.example.selectsmart_app.viewmodels.CartViewModel
import com.example.selectsmart_app.viewmodels.SearchViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// This fragment handles product searching and filtering.
// It allows the user to search for products, view filtered results,
// open product details, add products to basket, and save search history.
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Shared SearchViewModel used across fragments.
    // activityViewModels means the same SearchViewModel can be shared
    // between HomeFragment, SearchFragment and FilterFragment.
    private val viewModel: SearchViewModel by activityViewModels()
    // CartViewModel is used to add searched products to the basket.
    private val cartViewModel: CartViewModel by viewModels()
    // Adapter used to display product search results in the RecyclerView.
    private lateinit var productAdapter: ProductListAdapter
    // This flag prevents an infinite loop when the EditText is updated from LiveData.
    // It stops the TextWatcher from reacting when the text is being synced manually.
    private var isSyncingText = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Runs after the view has been created.
    // This is where the RecyclerView, observers and listeners are set up.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sets up the RecyclerView and product adapter.
        setupRecyclerView()
        // Observes LiveData from SearchViewModel and CartViewModel.
        observeViewModel()
        // Sets up button clicks and search input listener.
        setupListeners()
    }

    // This function sets up the RecyclerView and its adapter.
    // The RecyclerView displays the product search results.
    private fun setupRecyclerView() {
        // Creates the ProductListAdapter with an empty list at first.
        productAdapter = ProductListAdapter(emptyList(), { product ->
            // Runs when a product item is clicked.
            // It navigates to the Product Detail screen and passes the product ID.
            if (isAdded) {
                val bundle = Bundle().apply {
                    putString("productId", product.ProdId)
                }
                findNavController().navigate(R.id.productDetailFragment, bundle)
            }
        }, { product ->
            addToBasket(product)
        })

        // Sets the RecyclerView to display products in a vertical list.
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    // This function observes LiveData from the SearchViewModel and CartViewModel.
    // When the data changes, the UI updates automatically.
    private fun observeViewModel() {
        // Observes the filtered product list from SearchViewModel.
        viewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            // Hides the progress bar after results are loaded.
            binding.progressBar.visibility = View.GONE
            // Gets the current search query.
            val currentQuery = viewModel.searchQuery.value ?: ""
            // If no products are found after searching or filtering,
            // hide the RecyclerView and show the "No Results" message.
            if (products.isEmpty() && (currentQuery.isNotBlank() || viewModel.selectedCategory != null || viewModel.selectedBrand != null)) {
                binding.rvSearchResults.visibility = View.GONE
                binding.tvNoResults.visibility = View.VISIBLE
            // If products exist, update the adapter and show the results.
            } else {
                productAdapter.updateData(products)
                binding.rvSearchResults.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoResults.visibility = View.GONE
            }
        }

        // Observes the search query stored in SearchViewModel.
        // This keeps the search box synced when returning from the filter screen.
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text.toString() != query) {
                // Prevents TextWatcher from triggering another update while syncing.
                isSyncingText = true
                // Updates the search box text
                binding.etSearch.setText(query)
                // Moves the cursor to the end of the text.
                binding.etSearch.setSelection(query?.length ?: 0)
                // Re-enables normal TextWatcher behaviour.
                isSyncingText = false
            }
        }

        // Observes whether adding a product to basket was successful.
        cartViewModel.addToCartSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Added to basket", Toast.LENGTH_SHORT).show()
                cartViewModel.resetAddToCartStatus()
            }
        }

        cartViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                cartViewModel.resetAddToCartStatus()
            }
        }
    }

    /// This function sets up all click listeners and search input listeners.
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnFilter.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_filterFragment)
        }

        // TextWatcher listens for changes in the search box.
        // Each time the user types, the search query is updated in SearchViewModel.
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            // Not used, but required by TextWatcher.
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            // Runs whenever the text in the search box changes.
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Only update the ViewModel if the text is not being synced manually.
                if (!isSyncingText) {
                    viewModel.setSearchQuery(s.toString())
                }
            }
            // Not used, but required by TextWatcher.
            override fun afterTextChanged(s: Editable?) {}
        })

        // This listener runs when the user presses the search button on the keyboard.
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            // Checks if the keyboard action is the search action.
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Gets the entered search query.
                val query = textView.text.toString().trim()
                // Saves the search history only if the query is not empty.
                if (query.isNotEmpty()) {
                    saveSearchHistory(query)
                    // Hides the keyboard after the search is submitted.
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(textView.windowToken, 0)
                }
                true
            } else {
                false
            }
        }
    }

    /// This function saves the user's search query in Firestore.
    // The saved search history is later used for adaptive recommendations.
    private fun saveSearchHistory(query: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val searchHistory = SearchHistory(
            searchId = UUID.randomUUID().toString(),
            userId = userId,
            searchQuery = query,
            timestamp = Timestamp.now()
        )
        db.collection("SearchHistory").document(searchHistory.searchId).set(searchHistory)
    }

    // Function used to add product to basket
    private fun addToBasket(product: Product) {
        if (auth.currentUser != null) {
            cartViewModel.addToCart(product.ProdId, product.ProdPrice)
        } else {
            Toast.makeText(requireContext(), "Please login to add items", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}