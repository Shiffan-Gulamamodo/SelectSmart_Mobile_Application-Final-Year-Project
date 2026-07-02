package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.ItemFeaturedProductBinding
import com.example.selectsmart_app.models.Product

// This adpater class is used to display the featured products at home page
class FeaturedProductAdapter(
    private var products: List<Product>, //Contains the list of featured products
    private val onProductClick: (Product) -> Unit //Triggers the callback function on product click
) : RecyclerView.Adapter<FeaturedProductAdapter.FeaturedViewHolder>() {

    //ViewHolder class for featured product layout
    inner class FeaturedViewHolder(private val binding: ItemFeaturedProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) { // function which is used to bind product data
            // Displays product name and category
            binding.tvFeaturedName.text = product.ProdName
            binding.tvFeaturedCategory.text = product.CategoryId?.toString() ?: ""
            // Display product sign by using pound sign
            binding.tvFeaturedPrice.text = "£${product.ProdPrice}"
            //Load product image using the Glide library
            Glide.with(binding.ivFeaturedImage.context)
                .load(product.ProdImage)
                .into(binding.ivFeaturedImage)

            binding.root.setOnClickListener { onProductClick(product) }
        }
    }

    // This piece of code creates ViewHolder and inflates featured layout product
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeaturedViewHolder(binding)
    }
    //Bind data to viewHolder
    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        holder.bind(products[position])
    }
    // Updates the RecyclerView data when new featured products are loaded
    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged() // refreshes recycler view
    }
}
