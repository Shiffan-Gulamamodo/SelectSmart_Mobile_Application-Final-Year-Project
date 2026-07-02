package com.example.selectsmart_app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentProductDetailBinding
import com.example.selectsmart_app.models.UserInteraction
import com.example.selectsmart_app.viewmodels.CartViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// This fragment displays the full details of a selected product.
// It allows users to view product information, add the product to basket,
// compare the product, and stores viewed-product behaviour for recommendations.
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Shared CartViewModel used to manage cart actions across fragments.
    // activityViewModels means this ViewModel can be shared with other fragments
    // inside the same activity.
    private val cartViewModel: CartViewModel by activityViewModels()

    // Stores the current product price so it can be used when adding to basket.
    private var currentPrice: Double = 0.0
    private var currentCategoryId: String = ""
    private var currentProductId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        // Gets the selected product ID from the navigation arguments.
        currentProductId = arguments?.getString("productId") ?: ""
        if (currentProductId.isNotEmpty()) {
            loadProductDetails(currentProductId)
            loadReviewSummary(currentProductId)
        }

        binding.btnAddToBasket.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "Please login to add items to basket", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (currentProductId.isNotEmpty()) {
                cartViewModel.addToCart(currentProductId, currentPrice)
            } else {
                Toast.makeText(context, "Product error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCompare.setOnClickListener {
            // If the category is available, pass the product ID and category ID
            // to the CompareProductsFragment.
            if (currentCategoryId.isNotEmpty()) {
                val bundle = Bundle().apply {
                    putString("productId1", currentProductId)
                    putString("categoryId", currentCategoryId)
                }
                findNavController().navigate(R.id.action_productDetailFragment_to_compareProductsFragment, bundle)
            } else {
                Toast.makeText(context, "Product category not found", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()
    }

    // This function tracks when a user views a product.
    // The data is saved in the UserInteractions collection in Firestore
    // and later used for recently viewed recommendations.
    private fun trackViewInteraction(productId: String, categoryId: String) {
        val userId = auth.currentUser?.uid ?: return
        val interactionId = UUID.randomUUID().toString()
        val interaction = UserInteraction(
            interactionId = interactionId,
            userId = userId,
            prodId = productId,
            interactionType = "view",
            interactionTime = Timestamp.now(),
            categoryId = categoryId
        )
        db.collection("UserInteractions").document(interactionId).set(interaction)
    }

    // This function observes CartViewModel LiveData.
    // It updates the UI based on cart success, error messages and loading state.
    private fun observeViewModel() {
        cartViewModel.addToCartSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Added to basket", Toast.LENGTH_SHORT).show()
                cartViewModel.resetAddToCartStatus()
                // Reloads the cart to make sure the latest cart data is available.
                cartViewModel.loadCart()
                findNavController().navigate(R.id.action_productDetailFragment_to_cartFragment)
            }
        }

        cartViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                cartViewModel.resetAddToCartStatus()
            }
        }
        // Observes loading state while a product is being added to the basket.
        cartViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Disables the button during loading to prevent repeated clicks.
            binding.btnAddToBasket.isEnabled = !isLoading
            // Changes button transparency to show loading/disabled state.
            binding.btnAddToBasket.alpha = if (isLoading) 0.7f else 1.0f
        }
    }

    // This function loads the selected product details from Firestore.
    // It retrieves product name, price, rating, description, image,
    // stock status, specifications and category ID.
    private fun loadProductDetails(productId: String) {
        db.collection("Products").document(productId).get().addOnSuccessListener { document ->
            if (document != null && _binding != null && isAdded) {
                val name = document.getString("ProdName") ?: ""
                val price = (document.get("ProdPrice") as? Number)?.toDouble() ?: 0.0
                val rating = (document.get("ProdRating") as? Number)?.toDouble() ?: 0.0
                val desc = document.getString("ProdDescription") ?: ""
                val image = document.getString("ProdImage") ?: ""
                val stock = document.getLong("ProdStock") ?: 0L

                // Retrieves product specifications as a map.
                // Different products can have different specification fields.
                @Suppress("UNCHECKED_CAST")
                val specs = document.get("ProdSpecification") as? Map<String, Any>
                currentCategoryId = document.getString("CategoryId") ?: ""

                binding.tvProductName.text = name
                currentPrice = price
                binding.tvProductPrice.text = "£${String.format("%.2f", currentPrice)}"

                // Formats and displays product rating.
                //This line converts rating into 1 decimal place
                val formattedRating = String.format(Locale.ROOT, "%.1f", rating)
                //Shows rating number as text near the rating bar
                binding.ratingBar.rating = rating.toFloat()
                binding.tvRatingValue.text = formattedRating
                binding.bottomRatingBar.rating = rating.toFloat()
                binding.tvAverageRating.text = formattedRating

                binding.tvProductDescription.text = desc

                if (stock > 0) {
                    binding.tvStockStatus.text = "In Stock"
                    binding.tvStockStatus.setTextColor(Color.parseColor("#008A00"))
                    binding.btnAddToBasket.isEnabled = true
                    binding.btnAddToBasket.alpha = 1.0f
                } else {
                    binding.tvStockStatus.text = "Out of Stock"
                    binding.tvStockStatus.setTextColor(Color.RED)
                    binding.btnAddToBasket.isEnabled = false
                    binding.btnAddToBasket.alpha = 0.5f
                }

                // Displays product specifications dynamically.
                renderSpecifications(specs)

                // Loads the product image into the ImageView using Glide
                Glide.with(this)
                    .load(image)
                    .into(binding.ivProductImage)

                // Track interaction after category is loaded
                trackViewInteraction(productId, currentCategoryId)
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error loading product: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // This function loads reviews for the current product
    // and calculates the average rating.
    private fun loadReviewSummary(productId: String) {
        db.collection("Reviews")
            .whereEqualTo("ProdId", productId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Checks that the snapshot exists and the fragment is still active.
                if (snapshot != null && _binding != null && isAdded) {
                    // Counts how many reviews exist for the product.
                    val count = snapshot.size()
                    // Only calculate average if reviews exist.
                    if (count > 0) {
                        var totalRating = 0.0
                        // Adds all review ratings together.
                        for (doc in snapshot.documents) {
                            totalRating += doc.getDouble("Rating") ?: 0.0
                        }
                        // Calculates the average rating.
                        val average = totalRating / count
                        val formattedRating = String.format(Locale.ROOT, "%.1f", average)

                        binding.ratingBar.rating = average.toFloat()
                        binding.tvRatingValue.text = formattedRating
                        binding.tvAverageRating.text = formattedRating
                        binding.bottomRatingBar.rating = average.toFloat()
                    }
                }
            }
    }

    // This function dynamically displays product specifications.
    // It creates rows at runtime based on the specification map from Firestore.
    private fun renderSpecifications(specs: Map<String, Any>?) {
        // Clears old specification rows before adding new ones.
        binding.specsContainer.removeAllViews()
        if (specs == null) return

        // Loops through each specification key and value.
        for ((key, value) in specs) {
            // Creates a horizontal row layout for one specification.
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 12, 0, 12)
            }

            // Creates a TextView for the specification name,
            // such as RAM, Storage, Processor or Battery.
            val tvKey = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = key
                setTextColor(Color.parseColor("#565959"))
                textSize = 14f
            }

            // Creates a TextView for the specification value,
            // such as 8GB, 256GB, Intel i5, etc.
            val tvValue = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                text = value.toString()
                setTextColor(Color.BLACK)
                textSize = 14f
                setPadding(16, 0, 0, 0)
            }
            // Adds the key and value TextViews into the row.
            row.addView(tvKey)
            row.addView(tvValue)
            // Adds the row into the specifications container on the screen.
            binding.specsContainer.addView(row)

            val divider = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            binding.specsContainer.addView(divider)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}