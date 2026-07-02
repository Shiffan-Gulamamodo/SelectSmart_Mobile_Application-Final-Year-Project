package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentFilterBottomSheetBinding
import com.example.selectsmart_app.viewmodels.SearchViewModel

// This fragment is used to display and apply product filters.
// It allows the user to filter products by category, brand, price range,
// and sort order such as price low-to-high or high-to-low.
class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    // Use activityViewModels to share the same instance with SearchFragment
    // This means filters selected here can update the search results screen
    private val viewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    // This is where observers, current filter values and button clicks are set up.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observes category and brand filter data from SearchViewModel.
        observeViewModel()
        // Displays any filters that were already selected before opening this screen.
        setupCurrentValues()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Apply button collects selected filter values and sends them to SearchViewModel.
        binding.btnApply.setOnClickListener {
            val selectedCategory = binding.spinnerCategory.selectedItem?.toString()
            val selectedBrand = binding.spinnerBrand.selectedItem?.toString()
            val minPrice = binding.etMinPrice.text.toString().toDoubleOrNull()
            val maxPrice = binding.etMaxPrice.text.toString().toDoubleOrNull()

            // Checks which sort chip is selected.
            // If high-to-low is selected, products are sorted by highest price first.
            // Otherwise, products are sorted by lowest price first.
            val sortOrder = if (binding.cgSort.checkedChipId == R.id.chipPriceHighLow) {
                SearchViewModel.SortOrder.PRICE_HIGH_LOW
            } else {
                SearchViewModel.SortOrder.PRICE_LOW_HIGH
            }

            // Sends the selected filters to SearchViewModel.
            // SearchViewModel then updates the filtered product results.
            viewModel.setFilters(selectedCategory, selectedBrand, minPrice, maxPrice, sortOrder)
            // Returns back to the SearchFragment after applying filters.
            findNavController().popBackStack()
        }
    }

    // This function observes filter data from SearchViewModel.
    // It fills the category and brand spinners with available options.
    private fun observeViewModel() {
        // Observes the list of available categories.
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            // Creates a list with "All" at the start.
            // This lets the user choose no specific category filter.
            val list = mutableListOf("All")
            list.addAll(categories)
            // Creates an adapter to display category values inside the spinner.
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, list)
            // Sets the dropdown layout for the spinner.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Attaches the adapter to the category spinner.
            binding.spinnerCategory.adapter = adapter

            // Gets the currently selected category from the ViewModel.
            // If no category is selected, "All" is used.
            val currentCat = viewModel.selectedCategory ?: "All"
            // Finds the index of the current category in the spinner list.
            val index = list.indexOf(currentCat)
            // If the category exists in the list, set it as selected.
            if (index >= 0) binding.spinnerCategory.setSelection(index)
        }

        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            val list = mutableListOf("All")
            list.addAll(brands)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, list)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerBrand.adapter = adapter

            val currentBrand = viewModel.selectedBrand ?: "All"
            val index = list.indexOf(currentBrand)
            if (index >= 0) binding.spinnerBrand.setSelection(index)
        }
    }

    // This function displays the current filter values when the filter screen opens.
    // This helps the user see what filters are already active.
    private fun setupCurrentValues() {
        // Sets the selected sort chip based on the current sort order in SearchViewModel.
        if (viewModel.sortBy == SearchViewModel.SortOrder.PRICE_HIGH_LOW) {
            binding.chipPriceHighLow.isChecked = true
        } else {
            binding.chipPriceLowHigh.isChecked = true
        }

        // Displays the current minimum price if it exists.
        viewModel.minPrice?.let { binding.etMinPrice.setText(it.toString()) }
        // Displays the current maximum price if it exists.
        viewModel.maxPrice?.let { binding.etMaxPrice.setText(it.toString()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
