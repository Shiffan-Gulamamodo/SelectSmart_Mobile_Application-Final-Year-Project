package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.ItemCategoryBinding
import com.example.selectsmart_app.models.Category

// This adapter class is used to display product categories
class CategoryAdapter(
    // Contains list of all category objects
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit //Callback function triggered when category is clicked
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    //ViewHolder class for category layout
    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) { //function to bind category data
            binding.tvCategoryName.text = category.CategoryType
            
            // Load category image from database URL by using Glide
            if (category.CategoryImage.isNotEmpty()) {
                Glide.with(binding.ivCategoryIcon.context)
                    .load(category.CategoryImage)
                    .placeholder(com.google.android.material.R.drawable.navigation_empty_icon)
                    .into(binding.ivCategoryIcon) //displays image in ImageView
            }

            binding.root.setOnClickListener { onCategoryClick(category) }
        }
    }
    // This function creates the ViewHolder and inflates category item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    //Binds category data
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    //Returns total number of categories
    override fun getItemCount(): Int = categories.size
}
