package com.example.selectsmart_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.selectsmart_app.R
import com.example.selectsmart_app.databinding.FragmentFilterBottomSheetBinding
import com.example.selectsmart_app.viewmodels.SearchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// This BottomSheetDialogFragment is used to display search filters and sorting options.
// It appears as a bottom sheet instead of a full screen.
// Users can filter products by category, brand, price range and sorting order.
class FilterBottomSheetFragment(

    // SearchViewModel is passed into this bottom sheet so it can update
    // the same search/filter data used by the search screen.
    private val viewModel: SearchViewModel,

    // onApply is a callback function that runs after the user applies filters.
    // This allows the parent screen to refresh or react after filters are applied.
    private val onApply: () -> Unit

) : BottomSheetDialogFragment() {

    // ViewBinding variable for fragment_filter_bottom_sheet.xml.
    // It is nullable so it can be cleared when the view is destroyed.
    private var _binding: FragmentFilterBottomSheetBinding? = null

    // Safe access to the binding object while the fragment view exists.
    private val binding get() = _binding!!

    // Creates and loads the bottom sheet layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflates/loads fragment_filter_bottom_sheet.xml using ViewBinding.
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)

        // Returns the root layout to be displayed inside the bottom sheet.
        return binding.root
    }

    // Runs after the view has been created.
    // This is where observers, existing filter values and button clicks are set up.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observes category and brand lists from the SearchViewModel.
        setupObservers()

        // Displays any previously selected filter values.
        setupCurrentValues()

        // Back button closes the bottom sheet without applying changes.
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        // Apply button collects the selected filter values
        // and sends them to the SearchViewModel.
        binding.btnApply.setOnClickListener {

            // Gets selected category from the spinner.
            // If nothing is selected, "All" is used.
            val selectedCategory = binding.spinnerCategory.selectedItem?.toString() ?: "All"

            // Gets selected brand from the spinner.
            // If nothing is selected, "All" is used.
            val selectedBrand = binding.spinnerBrand.selectedItem?.toString() ?: "All"

            // Gets minimum price entered by the user.
            // toDoubleOrNull prevents crashes if the input is empty or invalid.
            val minPrice = binding.etMinPrice.text.toString().toDoubleOrNull()

            // Gets maximum price entered by the user.
            val maxPrice = binding.etMaxPrice.text.toString().toDoubleOrNull()

            // Checks which sorting chip is selected.
            // If the high-to-low chip is selected, products are sorted by highest price first.
            // Otherwise, products are sorted from lowest price to highest price.
            val sortOrder = if (binding.cgSort.checkedChipId == R.id.chipPriceHighLow) {
                SearchViewModel.SortOrder.PRICE_HIGH_LOW
            } else {
                SearchViewModel.SortOrder.PRICE_LOW_HIGH
            }

            // Sends the selected filters to the SearchViewModel.
            // This updates the filtered product results.
            viewModel.setFilters(
                selectedCategory,
                selectedBrand,
                minPrice,
                maxPrice,
                sortOrder
            )

            // Runs the callback function after applying filters.
            onApply()

            // Closes the bottom sheet after filters are applied.
            dismiss()
        }
    }

    // This function observes available categories and brands from SearchViewModel.
    // It then displays them inside the category and brand spinners.
    private fun setupObservers() {

        // Observes the category list.
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->

            // Creates a mutable list and adds "All" as the first option.
            // "All" means no specific category filter is applied.
            val categories = mutableListOf("All")
            categories.addAll(categoryList)

            // Creates an ArrayAdapter to display categories inside the spinner.
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
            )

            // Sets the dropdown style for the spinner.
            categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            // Attaches the adapter to the category spinner.
            binding.spinnerCategory.adapter = categoryAdapter

            // Finds the previously selected category from the ViewModel.
            val catIndex = categoryAdapter.getPosition(
                viewModel.selectedCategory ?: "All"
            )

            // If the previous category exists in the list, select it.
            if (catIndex >= 0) {
                binding.spinnerCategory.setSelection(catIndex)
            }
        }

        // Observes the brand list.
        viewModel.brands.observe(viewLifecycleOwner) { brandList ->

            // Creates a mutable list and adds "All" as the first option.
            // "All" means no specific brand filter is applied.
            val brands = mutableListOf("All")
            brands.addAll(brandList)

            // Creates an ArrayAdapter to display brands inside the spinner.
            val brandAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                brands
            )

            // Sets the dropdown style for the spinner.
            brandAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            // Attaches the adapter to the brand spinner.
            binding.spinnerBrand.adapter = brandAdapter

            // Finds the previously selected brand from the ViewModel.
            val brandIndex = brandAdapter.getPosition(
                viewModel.selectedBrand ?: "All"
            )

            // If the previous brand exists in the list, select it.
            if (brandIndex >= 0) {
                binding.spinnerBrand.setSelection(brandIndex)
            }
        }
    }

    // This function displays the previously selected filter values.
    // This helps the user see their current filter settings when reopening the bottom sheet.
    private fun setupCurrentValues() {

        // Sets the correct price sorting chip based on the ViewModel's current sort order.
        if (viewModel.sortBy == SearchViewModel.SortOrder.PRICE_HIGH_LOW) {
            binding.chipPriceHighLow.isChecked = true
        } else {
            binding.chipPriceLowHigh.isChecked = true
        }

        // Displays the current minimum price if it exists.
        viewModel.minPrice?.let {
            binding.etMinPrice.setText(it.toString())
        }

        // Displays the current maximum price if it exists.
        viewModel.maxPrice?.let {
            binding.etMaxPrice.setText(it.toString())
        }
    }

    // Runs when the bottom sheet view is destroyed.
    // Setting binding to null helps prevent memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

