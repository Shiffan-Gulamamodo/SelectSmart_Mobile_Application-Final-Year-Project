package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.selectsmart_app.databinding.ItemOrderHistoryBinding
import com.example.selectsmart_app.models.Order
import java.text.SimpleDateFormat
import java.util.*

// This adapter class is used to display user order history
class OrderHistoryAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    // ViewHolder class for order history layout
    class OrderViewHolder(private val binding: ItemOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {  // function used to bind order data
            binding.tvOrderId.text = "Order #${order.orderId.takeLast(8).uppercase()}" // display shortened order ID
            binding.tvOrderStatus.text = order.orderStatus
            binding.tvOrderTotal.text = "£${String.format("%.2f", order.totalPrice)}"

            // format order date into readable format
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = order.orderDate?.toDate()
            // displays formatted order date
            binding.tvOrderDate.text = if (date != null) "Placed on ${dateFormat.format(date)}" else "Date unknown"

            // displays the product names and quantities for each order item
            if (order.items.isNotEmpty()) {
                val itemsText = StringBuilder()
                order.items.forEach { item ->
                    // Appends quantity and product name
                    itemsText.append("${item.quantity}x ${item.prodName}\n")
                }
                // displays the list of order item
                binding.tvOrderItems.text = itemsText.toString().trim()
            } else {
                // displays message if no order items exist
                binding.tvOrderItems.text = "No items found"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    // returns the total number of orders
    override fun getItemCount(): Int = orders.size

    // This updates RecyclerView when new order data is loaded
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
