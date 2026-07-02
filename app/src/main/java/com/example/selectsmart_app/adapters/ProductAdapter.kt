package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.ItemProductBinding
import com.example.selectsmart_app.models.Product

// This adapter class is used to display products
class ProductAdapter(
    private var products: List<Product>, //List of product objects
    private val onProductClick: (Product) -> Unit //triggers callback when a product is clicked
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // function used to bind product data to layout
        fun bind(product: Product) {
            binding.tvProductName.text = product.ProdName //displays product name
            binding.tvProductPrice.text = "£${product.ProdPrice}"
            binding.tvProductRating.text = product.ProdRating.toString()
            
            // Set rating value from Firestore
            binding.rbProduct.rating = product.ProdRating.toFloat()
            
            Glide.with(binding.ivProductImage.context)
                .load(product.ProdImage)
                .into(binding.ivProductImage)

            binding.root.setOnClickListener { onProductClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    // Binds product data to ViewHolder
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    // returns the total number of products
    override fun getItemCount(): Int = products.size

    // Updates RecyclerView when new product data is loaded
    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
