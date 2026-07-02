package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.ItemProductListBinding
import com.example.selectsmart_app.models.Product

// Ths adapter class used to display products in list layout
class ProductListAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onAddToBasketClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductListAdapter.ProductListViewHolder>() {

    inner class ProductListViewHolder(private val binding: ItemProductListBinding) :
        RecyclerView.ViewHolder(binding.root) {
            // This function is used to bind product data to layout
        fun bind(product: Product) {
            binding.tvProductName.text = product.ProdName
            binding.tvProductPrice.text = "£${product.ProdPrice}"
            binding.tvProductRating.text = product.ProdRating.toString()
            binding.rbProduct.rating = product.ProdRating.toFloat()
            
            // format product specifications from database
            if (product.ProdSpecification.isNotEmpty()) {
                val specText = StringBuilder()
                // Displays first 4 specifications
                product.ProdSpecification.entries.take(4).forEach { entry ->
                    specText.append("• ${entry.value}\n")
                }
                //displays formatted text
                binding.tvProductSpecs.text = specText.toString().trim()
            } else {
                // displays message if no specifications exist
                binding.tvProductSpecs.text = "No specifications available"
            }

            Glide.with(binding.ivProductImage.context)
                .load(product.ProdImage)
                .into(binding.ivProductImage)

            binding.btnViewProduct.setOnClickListener { onProductClick(product) } // Trigger product details screen
            binding.btnAddToBasket.setOnClickListener { onAddToBasketClick(product) }
            binding.root.setOnClickListener { onProductClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductListViewHolder {
        val binding = ItemProductListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductListViewHolder, position: Int) {
        holder.bind(products[position])
    }

    // Returns total number of products
    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
