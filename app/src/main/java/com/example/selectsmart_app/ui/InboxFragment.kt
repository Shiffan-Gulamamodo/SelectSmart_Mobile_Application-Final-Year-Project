package com.example.selectsmart_app.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.selectsmart_app.adapters.NotificationAdapter
import com.example.selectsmart_app.databinding.FragmentInboxBinding
import com.example.selectsmart_app.repositories.NotificationRepository
import kotlinx.coroutines.launch

// InboxFragment displays the user's notifications/inbox.
// It shows order status notifications in a RecyclerView,
// allows users to open related orders, mark notifications as read,
// and delete notifications
class InboxFragment : Fragment() {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    // NotificationRepository handles notification operations,
    // such as retrieving, marking as read, and deleting notifications.
    private val repository = NotificationRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Runs after the fragment view has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button returns the user to the previous screen.
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Creates the notification adapter.
        // It handles notification clicks and delete button clicks.
        val adapter = NotificationAdapter(
            // Runs when the user clicks a notification.
            onNotificationClick = { notification ->
                // Marks the clicked notification as read in Firestore.
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.markAsRead(notification.notificationId)
                }

                // If the notification is linked to an order,
                // navigate to the Order Success/Status screen for that order.
                if (notification.orderId != null) {
                    val action = InboxFragmentDirections.actionInboxFragmentToOrderSuccessFragment(notification.orderId!!)
                    findNavController().navigate(action)
                }
            },
            // Runs when the user clicks the delete button on a notification.
            onDeleteClick = { notification ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Deletes the selected notification from Firestore.
                    repository.deleteNotification(notification.notificationId)
                    // Shows a confirmation message after deletion.
                    Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )
        // Sets the RecyclerView to display notifications in a vertical list.
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        // Connects the adapter to the RecyclerView.
        binding.rvNotifications.adapter = adapter


        // Observe notification list in real time
        // lifecycleScope runs the coroutine while the fragment lifecycle is active.
        viewLifecycleOwner.lifecycleScope.launch {
            // getNotifications() returns a Flow from NotificationRepository.
            // flowWithLifecycle makes sure collection only happens when the lifecycle is active.
            repository.getNotifications()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { notifications ->
                    // If the binding is null, stop processing to avoid crashes.
                    if (_binding == null) return@collect

                    // If there are no notifications, show the empty inbox message.
                    if (notifications.isEmpty()) {
                        binding.tvEmptyInbox.visibility = View.VISIBLE
                        binding.rvNotifications.visibility = View.GONE
                    // If notifications exist, show the RecyclerView and update the adapter.
                    } else {
                        binding.tvEmptyInbox.visibility = View.GONE
                        binding.rvNotifications.visibility = View.VISIBLE
                        // Updates the RecyclerView list with the latest notifications.
                        adapter.submitList(notifications)
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
