package com.example.logix.user_fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.logix.R
import com.example.logix.adapter.LogoAdapter
import com.example.logix.viewmodel.FavoriteViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchButton: AppCompatButton  // Changed from MaterialButton to AppCompatButton
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    // Variables to store selected values
    lateinit var selectedCategory: String
    lateinit var selectedShape: String
    lateinit var selectedStyle: String
    lateinit var selectedColor: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        initializeViews(view)

        // Setup dropdowns with listeners
        setupDropdownsWithListeners(view)

        // Setup search button
        setupSearchButton()

        return view
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchButton = view.findViewById(R.id.search)  // This is AppCompatButton in XML
    }

    private fun setupDropdownsWithListeners(view: View) {
        // Dropdown 1 - Professions
        val dropdown1: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown1)
        val professions = arrayOf(
            "IT",
            "BUSINESS",
            "FINANCE",
            "EDUCATION",
            "HEALTH",
            "GAMING",
            "ECOMMERCE",
            "TRAVEL",
            "FOOD",
            "ENTERTAINMENT",
        )
        val adapter1 =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, professions)
        dropdown1.setAdapter(adapter1)

        // Set listener for dropdown1
        dropdown1.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = professions[position]
        }

        // Dropdown 2 - Shapes
        val dropdown2: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown2)
        val shapes = arrayOf(
            "CIRCLE",
            "SQUARE",
            "TRIANGLE",
            "HEXAGON",
            "RECTANGLE",
            "ABSTRACT",
            "SYMBOL",
        )
        val adapter2 =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, shapes)
        dropdown2.setAdapter(adapter2)

        // Set listener for dropdown2
        dropdown2.setOnItemClickListener { _, _, position, _ ->
            selectedShape = shapes[position]
        }

        // Dropdown 3 - Styles
        val dropdown3: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown3)
        val styles = arrayOf(
            "MINIMAL",
            "MODERN",
            "CLASSIC",
            "FUTURISTIC",
            "PLAYFUL",
            "LUXURY",
            "RETRO",
            "ABSTRACT",
        )
        val adapter3 =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, styles)
        dropdown3.setAdapter(adapter3)

        // Set listener for dropdown3
        dropdown3.setOnItemClickListener { _, _, position, _ ->
            selectedStyle = styles[position]
        }

        // Dropdown 4 - Colors
        val dropdown4: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown4)
        val colors = arrayOf(
            "Red",
            "Blue",
            "Green",
            "Yellow",
            "Orange",
            "Purple",
            "Pink",
            "Brown",
            "Black",
            "White",
            "Gray",
            "Gold",
            "Silver",
            "Bronze",
            "Multicolor",
            "Pastel",
            "Neon",
            "Dark",
            "Light"
        )
        val adapter4 =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colors)
        dropdown4.setAdapter(adapter4)

        // Set listener for dropdown4
        dropdown4.setOnItemClickListener { _, _, position, _ ->
            selectedColor = colors[position]
        }
    }

    private fun setupSearchButton() {
        searchButton.setOnClickListener {
            // Check if all categories are selected (not "All" options)
            if (areAllCategoriesSelected()) {
                showRecyclerView()
                setupRecyclerView()
            } else {
                // Optional: Show a message if not all categories are selected
                Toast.makeText(
                    requireContext(),
                    "Please select all categories to search",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun areAllCategoriesSelected(): Boolean {
        return !selectedCategory.contains("All") &&
                !selectedShape.contains("All") &&
                !selectedStyle.contains("All") &&
                !selectedColor.contains("All")
    }

    private fun showRecyclerView() {
        recyclerView.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        // 11 logos - using converted SVG (ic_logo) instead of logo2
        val logos = listOf(
            R.raw.logo1,
            R.raw.logo2,
            R.raw.logo6,
            R.raw.logo7,
            R.raw.logo2,
            R.raw.logo1,
            R.raw.logo2,
            R.raw.logo6,
            R.raw.logo7,
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = LogoAdapter(logos, favoriteViewModel)

        // Optional: Scroll to top when new results appear
        recyclerView.scrollToPosition(0)
    }
}