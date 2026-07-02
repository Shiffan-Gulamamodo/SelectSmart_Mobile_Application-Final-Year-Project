package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentOrderSuccessBinding
import com.example.selectsmart_app.viewmodels.OrderViewModel

// This fragment displays the order success screen after checkout.
// It shows the order number, total amount, delivery details,
// payment method and live order status progress.
class OrderSuccessFragment : Fragment() {

    private var _binding: FragmentOrderSuccessBinding? = null
    private val binding get() = _binding!!

    // navArgs is used to receive the orderId passed from CheckoutFragment.
    // This orderId is used to load the correct order details.
    private val args: OrderSuccessFragmentArgs by navArgs()

    // OrderViewModel is used to load order details,
    // delivery details, payment details and order status.
    private val orderViewModel: OrderViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Runs after the fragment view has been created.
    // This is where the order number is displayed, listeners are set up,
    // ViewModel observers are connected, and order details are loaded.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Displays the order ID received from CheckoutFragment.
        binding.tvOrderNumber.text = "Order #${args.orderId}"

        // Sets up button click actions.
        setupListeners()
        // Observes order data from OrderViewModel.
        observeViewModel()

        // Loads the order details using the order ID passed through navigation.
        orderViewModel.fetchOrderDetails(args.orderId)
    }

    // This function sets up click listeners for the buttons on the screen.
    private fun setupListeners() {
        // Back button takes the user back to the Home screen.
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_orderSuccessFragment_to_homeFragment)
        }

        // Continue Shopping button also takes the user back to the Home screen.
        binding.btnContinueShopping.setOnClickListener {
            findNavController().navigate(R.id.action_orderSuccessFragment_to_homeFragment)
        }
    }

    // This function observes LiveData from OrderViewModel.
    // When order, delivery, payment or status data changes,
    // the UI is updated automatically.
    private fun observeViewModel() {
        // Observes the main order details.
        orderViewModel.order.observe(viewLifecycleOwner) { order ->
            order?.let {
                // Displays the total order amount.
                binding.tvTotalAmount.text = String.format("Total: £%.2f", it.totalPrice)
            }
        }

        // Observes delivery information linked to the order.
        orderViewModel.delivery.observe(viewLifecycleOwner) { delivery ->
            delivery?.let {
                // Displays the customer's delivery name.
                binding.tvDeliveryName.text = it.fullName
                // Displays the delivery address.
                binding.tvDeliveryAddress.text = "${it.streetName}, ${it.city}, ${it.postCode}"
            }
        }

        // Observes payment information linked to the order.
        orderViewModel.payment.observe(viewLifecycleOwner) { payment ->
            payment?.let {
                // Displays the payment method used for the order.
                binding.tvPaymentMethod.text = it.paymentMethod
            }
        }

        // Observes the current order status.
        // Example: Pending, Processing, Delivering, Delivered.
        orderViewModel.orderStatus.observe(viewLifecycleOwner) { status ->
            // Updates the status text shown on the screen.
            binding.tvCurrentStatus.text = status
        }

        // Observes the progress value linked to the order status.
        orderViewModel.progress.observe(viewLifecycleOwner) { progress ->
            // Makes the progress bar determinate instead of loading endlessly.
            binding.statusProgress.isIndeterminate = false
            // Updates the progress bar value.
            binding.statusProgress.progress = progress
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}