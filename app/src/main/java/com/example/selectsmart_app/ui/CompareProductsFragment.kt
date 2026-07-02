package com.example.selectsmart_app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.selectsmart_app.databinding.FragmentCompareProductsBinding
import com.example.selectsmart_app.models.Product
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// This fragment is used to compare two technology products side-by-side.
// It allows the user to select two products from the same category
// and dynamically displays their specifications for comparison.
class CompareProductsFragment : Fragment() {

    private var _binding: FragmentCompareProductsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    // Stores the first selected product.
    private var product1: Product? = null
    private var product2: Product? = null
    // Stores the selected category ID.
    // This ensures the second product is from the same category as the first product.
    private var categoryId: String = ""
    // Stores all products loaded from Firestore.
    private var allProducts: List<Product> = emptyList()

    // Creates fragment view using ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompareProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after fragment view has been created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate back to previous screen
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Load all products from Firestore
        loadAllProducts()

        // Gets the product ID passed from ProductDetailFragment, if available.
        val initialProdId = arguments?.getString("productId1")
        val initialCategoryId = arguments?.getString("categoryId")

        // If the user came from a product detail page,
        // automatically load that product as product 1.
        if (initialProdId != null && initialCategoryId != null) {
            categoryId = initialCategoryId.trim()
            loadInitialProduct(initialProdId)
        }
    }

    // This function loads all products from the Products collection in Firestore.
    private fun loadAllProducts() {
        db.collection("Products").get().addOnSuccessListener { snapshot ->
            allProducts = snapshot.toObjects(Product::class.java)
            setupDropdown1()
            // If a category has already been selected,
            // set up the second product dropdown as well.
            if (categoryId.isNotEmpty()) {
                setupDropdown2()
            }
        }
    }

    // This function loads the initially selected product using its product ID.
    // This is used when the user clicks Compare from the Product Detail screen.
    private fun loadInitialProduct(id: String) {
        db.collection("Products").document(id).get().addOnSuccessListener { doc ->
            product1 = doc.toObject(Product::class.java)
            // Updates the screen with product 1 details.
            updateUI()
            // Sets up product 2 dropdown using the same category.
            setupDropdown2()
        }
    }

    // This function sets up the first product dropdown.
    // It lets the user choose the first product for comparison.
    private fun setupDropdown1() {
        if (allProducts.isEmpty()) return

        // Creates a list of product names for the dropdown.
        val productNames = allProducts.map { it.ProdName }
        // Creates an adapter to display product names in the dropdown.
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productNames)
        // Attaches the adapter to the first product dropdown.
        binding.spinnerProduct1.setAdapter(adapter)

        // Handles first product selection
        binding.spinnerProduct1.setOnItemClickListener { _, _, position, _ ->
            // Gets the selected product name.
            val selectedName = adapter.getItem(position)
            // Finds the matching Product object from the full product list.
            product1 = allProducts.find { it.ProdName == selectedName }
            product1?.let {
                // Stores the selected product category ID.
                categoryId = extractId(it.CategoryId) ?: ""
                // Clears product 2 because product 1 has changed.
                product2 = null
                binding.spinnerProduct2.setText("", false)
                // Updates the UI with product 1 details.
                updateUI()
                // Updates the second dropdown to only show products in the same category.
                setupDropdown2()
            }
        }
    }

    // This function sets up the second product dropdown.
    // It only shows products from the same category as product 1.
    private fun setupDropdown2() {
        // If there is no selected category or no products, disable product 2 dropdown.
        if (categoryId.isEmpty() || allProducts.isEmpty()) {
            binding.spinnerProduct2.isEnabled = false
            return
        }

        // Filters products so product 2 must be in the same category
        // and cannot be the same product as product 1.
        val filteredProducts = allProducts.filter { 
            extractId(it.CategoryId)?.equals(categoryId, ignoreCase = true) == true && it.ProdId != product1?.ProdId 
        }

        // If no matching products are found, disable the second dropdown.
        if (filteredProducts.isEmpty()) {
            binding.spinnerProduct2.isEnabled = false
            binding.spinnerProduct2.setHint("No other products found")
            return
        }

        // Enables the second dropdown when matching products are available.
        binding.spinnerProduct2.isEnabled = true
        binding.spinnerProduct2.setHint("Select Product 2")

        // Creates a list of product names for the second dropdown.
        val productNames = filteredProducts.map { it.ProdName }
        // Creates an adapter for the second dropdown.
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, productNames)
        // Attaches the adapter to the second product dropdown.
        binding.spinnerProduct2.setAdapter(adapter)

        // Handles the selection of the second product.
        binding.spinnerProduct2.setOnItemClickListener { _, _, position, _ ->
            // Gets the selected product name.
            val selectedName = adapter.getItem(position)
            // Finds the selected Product object from the filtered product list.
            product2 = filteredProducts.find { it.ProdName == selectedName }
            // Updates the UI to show the full comparison.
            updateUI()
        }
    }

    // This helper function extracts a clean Firestore document ID.
    // It handles normal Strings, Firestore paths, and DocumentReference values.
    private fun extractId(value: Any?): String? {
        return when (value) {
            // If the value is a path like "Category/abc123",
            // only the final ID is returned.
            is String -> if (value.contains("/")) value.substringAfterLast("/").trim() else value.trim()
            // If the value is a DocumentReference, return its document ID.
            is DocumentReference -> value.id
            // Otherwise, convert the value to a String.
            else -> value?.toString()?.trim()
        }
    }

    // This function updates the comparison screen UI.
    // It displays product prices, ratings, release years, images and specifications.
    private fun updateUI() {
        // Stops the function if the fragment is no longer attached
        // or the binding has been cleared.
        if (!isAdded || _binding == null) return

        // Displays product 1 details if product 1 has been selected.
        product1?.let { p ->
            binding.tvPrice1.text = "£${p.ProdPrice}"
            binding.rb1.rating = p.ProdRating.toFloat()
            binding.tvRating1.text = String.format(Locale.ROOT, "%.1f / 5.0", p.ProdRating)
            binding.tvYear1.text = p.ProdReleaseYear.toString()
            // Loads product 1 image using Glide.
            Glide.with(this).load(p.ProdImage).into(binding.ivProd1)
            // Removes any image color filter.
            binding.ivProd1.clearColorFilter()
            // Displays product 1 name in the dropdown.
            binding.spinnerProduct1.setText(p.ProdName, false)
        }

        // Displays product 2 details if product 2 has been selected.
        product2?.let { p ->
            binding.tvPrice2.text = "£${p.ProdPrice}"
            binding.rb2.rating = p.ProdRating.toFloat()
            binding.tvRating2.text = String.format(Locale.ROOT, "%.1f / 5.0", p.ProdRating)
            binding.tvYear2.text = p.ProdReleaseYear.toString()
            Glide.with(this).load(p.ProdImage).into(binding.ivProd2)
            binding.ivProd2.clearColorFilter()
            binding.spinnerProduct2.setText(p.ProdName, false)

            // Displays dynamic specification comparison.
            renderComparisonSpecs()
            // Shows comparison content once both products are selected.
            binding.comparisonContent.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
        } ?: run {
            // If product 2 is not selected yet,
            // hide comparison content and show the empty state message.
            binding.comparisonContent.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
        }
    }

    // This function dynamically compares product specifications.
    // It creates comparison rows at runtime based on the specification fields
    // stored in Firestore for both products.
    private fun renderComparisonSpecs() {
        // Clears old comparison rows before adding new ones.
        binding.dynamicSpecsContainer.removeAllViews()
        // Gets product 1 and product 2.
        // If either product is missing, stop the function.
        val p1 = product1 ?: return
        val p2 = product2 ?: return

        // Gets the specification maps for both products.
        val specs1 = p1.ProdSpecification
        val specs2 = p2.ProdSpecification

        // Gets all unique specification labels from both products.
        // This ensures no specification is missed.
        val allKeys = (specs1.keys + specs2.keys).sorted()

        // Loop through all specifications
        for (label in allKeys) {
            // Creates a vertical layout row for each specification
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            // Creates the specification label/header.
            // Example: RAM, Storage, Processor, Battery.
            val tvLabel = TextView(requireContext()).apply {
                text = label.uppercase()
                textSize = 11f
                setTextColor(Color.parseColor("#999999"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 12)
                letterSpacing = 0.1f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            // Adds the label to the row.
            row.addView(tvLabel)

            // Creates a horizontal layout to hold both product values side-by-side.
            val valueLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
            }

            // Gets the value for product 1.
            // If product 1 does not have that specification, "-" is shown
            val val1 = specs1[label]?.toString() ?: "-"
            val val2 = specs2[label]?.toString() ?: "-"

            // Creates the TextView for product 1 specification value.
            val tvVal1 = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = val1
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(8, 0, 8, 0)
            }

            // Creates a vertical divider between product 1 and product 2 values.
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.parseColor("#F0F0F0"))
            }

            val tvVal2 = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = val2
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(8, 0, 8, 0)
            }

            // Adds product 1 value, divider, and product 2 value into the horizontal layout.
            valueLayout.addView(tvVal1)
            valueLayout.addView(divider)
            valueLayout.addView(tvVal2)
            row.addView(valueLayout)

            // Creates a horizontal divider below each specification row.
            val bottomDivider = View(requireContext()).apply {
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                params.setMargins(0, 24, 0, 0)
                layoutParams = params
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            row.addView(bottomDivider)

            // Adds the completed specification comparison row to the screen.
            binding.dynamicSpecsContainer.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
