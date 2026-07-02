package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.ItemCartBinding
import com.example.selectsmart_app.models.CartItem
import com.example.selectsmart_app.models.Product
import java.util.Locale

// This is the adapter class  of the project which is used to display cart items inside Recyclerview
class CartAdapter(
    private val onQuantityChanged: (CartItem, Int) -> Unit, // Callback function used for triggering when the quantity changes
    private val onRemoveItem: (CartItem) -> Unit, // Callback function used for triggering when an item is removed
    private val productProvider: (String, (Product?) -> Unit) -> Unit // Callback function used to retrieve product details from Firestore
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    // This piece of code creates the viewHolder used to display cart items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }
    //Binds the cart item data to ViewHolder
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    // This is the ViewHolder class for cart item layout
    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // This Function here is used to display cart item information
        fun bind(cartItem: CartItem) {
            binding.tvQuantity.text = cartItem.quantity.toString() // Displays current quantity


            val totalPrice = cartItem.price * cartItem.quantity // Calculate the total price for quantity selected
            binding.tvProductPrice.text = String.format(Locale.UK, "£%.2f", totalPrice)

            // Retrieves product details using product ID from Firestore
            productProvider(cartItem.prodId) { product ->
                product?.let {
                    binding.tvProductName.text = it.ProdName
                    Glide.with(binding.ivProductImage.context)
                        .load(it.ProdImage)
                        .into(binding.ivProductImage)
                }
            }

            //Increases quantity on click
            binding.ibPlus.setOnClickListener {
                onQuantityChanged(cartItem, cartItem.quantity + 1)
            }

            //Decreases quantity on click
            binding.ibMinus.setOnClickListener {
                //Prevents it from going below 1
                if (cartItem.quantity > 1) {
                    onQuantityChanged(cartItem, cartItem.quantity - 1)
                }
            }
            //Removes item from cart
            binding.ibRemove.setOnClickListener {
                //Trigger remove item callback
                onRemoveItem(cartItem)
            }
        }
    }
    // DiffUtil used to improve RecyclerView performance
    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        //Checks for two same items in cart
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.cartItemId == newItem.cartItemId
        }
        //Check if cart item is changed
        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
