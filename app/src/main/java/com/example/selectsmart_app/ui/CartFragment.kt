package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.selectsmart_app.R
import com.example.selectsmart_app.adapters.CartAdapter
import com.example.selectsmart_app.databinding.FragmentCartBinding
import com.example.selectsmart_app.viewmodels.CartViewModel

//this file is for the cart fragment to manage cart functionality
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    // Shared CartViewModel used across fragments
    private val viewModel: CartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter

    // Creates fragment view using ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after fragment view has been created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        // Observe ViewModel LiveData
        observeViewModel()

        // Navigate back to previous screen
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Navigate to checkout screen
        binding.btnCheckout.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_checkoutFragment)
        }

        // Navigate back to home screen
        binding.btnStartShopping.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_homeFragment)
        }

        // Load cart items from Firestore
        viewModel.loadCart()
    }

    // Function used to setup RecyclerView and adapter
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            // Updates cart item quantity
            onQuantityChanged = { item, qty -> viewModel.updateQuantity(item, qty) },
            // Removes item from cart
            onRemoveItem = { item -> viewModel.removeItem(item) },
            // fetches product information
            productProvider = { id, callback -> viewModel.getProduct(id, callback) }
        )

        // Configure RecyclerView layout
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    // Function used to observe LiveData from ViewModel
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show or hide progress bar
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Refresh UI when loading finishes
            if (!isLoading) {
                // When loading finishes, ensure we refresh the UI with whatever items we have
                updateUI(viewModel.cartItems.value ?: emptyList())
            }
        }
        // Observe cart items
        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            // Only update if not currently loading to avoid flickering with empty states
            if (viewModel.isLoading.value == false) {
                updateUI(items)
            }
        }

        // Observe subtotal price updates
        viewModel.subtotal.observe(viewLifecycleOwner) { subtotal ->
            binding.tvSubtotal.text = String.format("£%.2f", subtotal)
        }

        // Observe shipping cost updates
        viewModel.shippingCost.observe(viewLifecycleOwner) { shipping ->
            binding.tvShipping.text = if (shipping == 0.0) "Free" else String.format("£%.2f", shipping)
        }

        // Observes total price updates
        viewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.tvTotalPrice.text = String.format("£%.2f", total)
        }
    }

    // Function used to update cart user interface
    private fun updateUI(items: List<com.example.selectsmart_app.models.CartItem>) {
        // Display empty cart layout if cart has no items
        if (items.isEmpty()) {
            binding.layoutEmptyCart.visibility = View.VISIBLE
            binding.rvCartItems.visibility = View.GONE
            binding.cvCheckout.visibility = View.GONE
            binding.cvCartBanner.visibility = View.GONE
        } else {
            // Display cart item layout if cart contains products
            binding.layoutEmptyCart.visibility = View.GONE
            binding.rvCartItems.visibility = View.VISIBLE
            binding.cvCheckout.visibility = View.VISIBLE
            binding.cvCartBanner.visibility = View.VISIBLE
            
            binding.tvCartCount.text = if (items.size == 1) {
                "You have 1 Item in your cart"
            } else {
                "You have ${items.size} Items in your cart"
            }
            
            cartAdapter.submitList(items)
        }
    }
    // Clears binding reference when fragment view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}