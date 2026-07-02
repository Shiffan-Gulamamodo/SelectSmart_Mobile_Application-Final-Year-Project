package com.example.selectsmart_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.selectsmart_app.databinding.ItemNotificationBinding
import com.example.selectsmart_app.models.Notification
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// This adapter class is used to display notifications i
class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit, // callback for notification
    private val onDeleteClick: (Notification) -> Unit //callback for delete
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder class for notification layout
    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message
            binding.tvNotificationTime.text = formatTimestamp(notification.timestamp)

            // Shows track order button only if notification contains order ID
            binding.btnTrackOrder.visibility = if (notification.orderId != null) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }

            binding.btnTrackOrder.setOnClickListener {
                onNotificationClick(notification)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(notification)
            }
            
            // Highlight unread notifications
            if (!notification.isRead) {
                //light pink for unread notification
                binding.root.setCardBackgroundColor(0xFFFCE4EC.toInt()) // Light pink tint for unread
            } else {
                //White for read notification
                binding.root.setCardBackgroundColor(0xFFFFFFFF.toInt())
            }
        }
        // This function here used to format Firebase timestamp into readable date
        private fun formatTimestamp(timestamp: Timestamp?): String {
            if (timestamp == null) return "" //returns empty when null
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) //date format for notification
            return sdf.format(timestamp.toDate()) //converts datatype
        }
    }

    // DiffUtil used to improve RecyclerView update performance
    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        // Checks whether two notifications are the same item
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }
        // Checks changes in notification contents
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}