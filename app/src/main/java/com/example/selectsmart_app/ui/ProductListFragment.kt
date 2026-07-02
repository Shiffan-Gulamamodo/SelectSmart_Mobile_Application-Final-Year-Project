package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.selectsmart_app.R
import com.example.selectsmart_app.adapters.ProductListAdapter
import com.example.selectsmart_app.databinding.FragmentProductListBinding
import com.example.selectsmart_app.viewmodels.CartViewModel
import com.example.selectsmart_app.viewmodels.ProductViewModel
import com.google.firebase.auth.FirebaseAuth

// This fragment displays products from a selected category.
// For example, if the user selects Smartphones, Laptops or Consoles,
// this screen shows all products from that category.
class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    // ProductViewModel is used to load products from the selected category.
    private val viewModel: ProductViewModel by viewModels()
    // CartViewModel is used to handle adding products to the basket/cart.
    private val cartViewModel: CartViewModel by viewModels()
    // navArgs is used to receive the category ID passed from the Home screen.
    // Example: "SmartPhones", "Laptops", or "Consoles".
    private val args: ProductListFragmentArgs by navArgs()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvTitle.text = args.categoryId
        
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        // Creates the adapter used to display the product list.
        // The adapter handles product item clicks and add-to-basket clicks.
        val adapter = ProductListAdapter(
            products = emptyList(),
            onProductClick = { product ->
                val action = ProductListFragmentDirections.actionProductListFragmentToProductDetailFragment(product.ProdId)
                findNavController().navigate(action)
            },
            onAddToBasketClick = { product ->
                if (auth.currentUser != null) {
                    cartViewModel.addToCart(product.ProdId, product.ProdPrice)
                } else {
                    Toast.makeText(requireContext(), "Please login to add items", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        // Observes the category product list from ProductViewModel.
        // When the products are loaded, the adapter is updated with the new data.
        viewModel.categoryProducts.observe(viewLifecycleOwner) { products ->
            adapter.updateData(products)
        }

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
        
        viewModel.loadAllProductsForCategory(args.categoryId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
