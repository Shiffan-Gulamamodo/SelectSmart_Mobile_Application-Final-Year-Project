package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.selectsmart_app.R
import com.example.selectsmart_app.adapters.CategoryAdapter
import com.example.selectsmart_app.adapters.FeaturedProductAdapter
import com.example.selectsmart_app.adapters.ProductAdapter
import com.example.selectsmart_app.databinding.FragmentHomeBinding
import com.example.selectsmart_app.repositories.NotificationRepository
import com.example.selectsmart_app.viewmodels.ProductViewModel
import com.example.selectsmart_app.viewmodels.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// This fragment controls the Home screen of the SelectSmart application.
// It displays featured products, categories, recommendations,
// recent search products, recently viewed products, and product sections.
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by viewModels()

    // SearchViewModel is shared across fragments using activityViewModels.
    // This allows the home screen and search screen to share search-related state.
    private val searchViewModel: SearchViewModel by activityViewModels()
    // FirebaseAuth is used to get the currently logged-in user's ID.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // NotificationRepository is used to retrieve user notifications from Firestore.
    private val notificationRepository = NotificationRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clear previous search when arriving at Home
        searchViewModel.clearSearch()

        // Sets up all RecyclerViews used on the Home screen.
        setupRecyclerViews()
        // Adds snapping effect to the featured product carousel.
        setupSnapHelper()
        // Sets up button/card click actions for navigation.
        setupClickListeners()
        // Observes product data from ProductViewModel.
        observeViewModel()
        // Observes unread notifications and updates the notification badge.
        setupNotificationObserver()

        val userId = auth.currentUser?.uid ?: ""
        viewModel.loadHomeData(userId)
    }

    // Function used to setup RecyclerViews
    private fun setupRecyclerViews() {
        binding.rvFeatured.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecommendations.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentSearches.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentlyViewed.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSmartPhones.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvLaptops.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvConsoles.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    // Function used to add snapping effect to featured products
    private fun setupSnapHelper() {
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvFeatured)
    }

    // Function used to setup navigation click listeners
    private fun setupClickListeners() {
        binding.cvProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.etSearch.setOnClickListener {
            searchViewModel.clearSearch()
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        // Makes the search input act like a clickable navigation button
        // instead of allowing typing directly on the Home screen.
        binding.etSearch.isFocusable = false
        binding.etSearch.isClickable = true

        binding.cvComparison.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_compareProductsFragment)
        }

        binding.cvFilters.setOnClickListener {
            searchViewModel.clearSearch()
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.llSearchNav.setOnClickListener {
            searchViewModel.clearSearch()
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.llCartNav.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cartFragment)
        }

        binding.llInboxNav.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_inboxFragment)
        }
    }

    // Function used to observe LiveData from ProductViewModel
    // When data changes, the RecyclerView adapters are updated automatically.
    private fun observeViewModel() {
        viewModel.featuredProducts.observe(viewLifecycleOwner) { products ->
            binding.rvFeatured.adapter = FeaturedProductAdapter(products) { product ->
                navigateToDetail(product.ProdId)
            }
        }

        // Section 1: Recommended for You (Based on Purchase History)
        viewModel.purchaseRecommendations.observe(viewLifecycleOwner) { products ->
            if (products.isNotEmpty()) {
                binding.llRecommendations.visibility = View.VISIBLE
                binding.rvRecommendations.adapter = ProductAdapter(products) { product ->
                    navigateToDetail(product.ProdId)
                }
            } else {
                binding.llRecommendations.visibility = View.GONE
            }
        }

        // Observes product categories and displays them in the category list.
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.rvCategories.adapter = CategoryAdapter(categories) { category ->
                // Passing CategoryType as it matches the CategoryId field in the Products collection
                navigateToList(category.CategoryType)
            }
        }

        // Section 2: Recent Searches (Based on Search History)
        viewModel.recentSearchProducts.observe(viewLifecycleOwner) { products ->
            if (products.isNotEmpty()) {
                binding.llRecentSearches.visibility = View.VISIBLE
                binding.rvRecentSearches.adapter = ProductAdapter(products) { product ->
                    navigateToDetail(product.ProdId)
                }
            } else {
                binding.llRecentSearches.visibility = View.GONE
            }
        }

        // Section 3: Recently Viewed (Based on Viewed Products)
        viewModel.recentlyViewed.observe(viewLifecycleOwner) { products ->
            if (products.isNotEmpty()) {
                binding.llRecentlyViewed.visibility = View.VISIBLE
                binding.rvRecentlyViewed.adapter = ProductAdapter(products) { product ->
                    navigateToDetail(product.ProdId)
                }
            } else {
                binding.llRecentlyViewed.visibility = View.GONE
            }
        }

        viewModel.smartPhones.observe(viewLifecycleOwner) { products ->
            binding.rvSmartPhones.adapter = ProductAdapter(products) { product ->
                navigateToDetail(product.ProdId)
            }
        }

        viewModel.laptops.observe(viewLifecycleOwner) { products ->
            binding.rvLaptops.adapter = ProductAdapter(products) { product ->
                navigateToDetail(product.ProdId)
            }
        }

        viewModel.consoles.observe(viewLifecycleOwner) { products ->
            binding.rvConsoles.adapter = ProductAdapter(products) { product ->
                navigateToDetail(product.ProdId)
            }
        }
    }

    // Function used to observe unread notifications
    private fun setupNotificationObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            notificationRepository.getNotifications().collect { notifications ->
                val unreadCount = notifications.count { !it.isRead }
                if (unreadCount > 0) {
                    binding.tvNotificationBadge.text = unreadCount.toString()
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                } else {
                    binding.tvNotificationBadge.visibility = View.GONE
                }
            }
        }
    }

    // Function used to navigate to product detail screen
    private fun navigateToDetail(productId: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(productId)
        findNavController().navigate(action)
    }

    // Function used to navigate to product list screen
    private fun navigateToList(categoryName: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToProductListFragment(categoryName)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}