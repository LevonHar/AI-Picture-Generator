package com.example.logix.user_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.logix.R
import com.example.logix.adapter.LogoNetworkAdapter
import com.example.logix.api.RetrofitClient
import com.example.logix.models.LogoItem
import com.example.logix.models.LogoSearchResponse
import com.example.logix.viewmodel.FavoriteViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchButton: AppCompatButton
    private lateinit var popularButton: AppCompatButton
    private lateinit var networkAdapter: LogoNetworkAdapter
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    private var selectedCategory: String? = null
    private var selectedShape: String? = null
    private var selectedStyle: String? = null
    private var selectedColor: String? = null

    private val baseUrl = "http://192.168.10.48:8080"
    private var isPopularMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initializeViews(view)
        setupRecyclerView()
        setupDropdownsWithListeners(view)
        setupSearchButton()
        setupPopularButton()

        return view
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchButton = view.findViewById(R.id.search)
        popularButton = view.findViewById(R.id.popular)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val favoriteIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()

        networkAdapter = LogoNetworkAdapter(
            logos = emptyList(),
            baseUrl = baseUrl,
            onFavoriteToggle = { logo, addToFav ->
                favoriteViewModel.toggleFavorite(logo, addToFav)
                val updatedFavIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()
                networkAdapter.updateFavoriteIds(updatedFavIds)
                Toast.makeText(
                    requireContext(),
                    if (addToFav) "Added to Favorites" else "Removed from Favorites",
                    Toast.LENGTH_SHORT
                ).show()
            },
            favoriteIds = favoriteIds,
            showCount = false
        )
        recyclerView.adapter = networkAdapter

        favoriteViewModel.favoriteNetworkLogos.observe(viewLifecycleOwner) { favorites ->
            val updatedFavIds = favorites.map { it.id }.toSet()
            networkAdapter.updateFavoriteIds(updatedFavIds)
        }
    }

    private fun setupDropdownsWithListeners(view: View) {
        val dropdown1: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown1)
        val categories = arrayOf(
            "IT", "BUSINESS", "FINANCE", "EDUCATION", "HEALTH",
            "GAMING", "ECOMMERCE", "TRAVEL", "FOOD", "ENTERTAINMENT"
        )
        dropdown1.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
        dropdown1.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            if (isPopularMode) exitPopularMode()
        }

        val dropdown2: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown2)
        val shapes = arrayOf("CIRCLE", "SQUARE", "TRIANGLE", "HEXAGON", "RECTANGLE", "ABSTRACT", "SYMBOL")
        dropdown2.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, shapes))
        dropdown2.setOnItemClickListener { _, _, position, _ ->
            selectedShape = shapes[position]
            if (isPopularMode) exitPopularMode()
        }

        val dropdown3: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown3)
        val styles = arrayOf("MINIMAL", "MODERN", "CLASSIC", "FUTURISTIC", "PLAYFUL", "LUXURY", "RETRO", "ABSTRACT")
        dropdown3.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, styles))
        dropdown3.setOnItemClickListener { _, _, position, _ ->
            selectedStyle = styles[position]
            if (isPopularMode) exitPopularMode()
        }

        val dropdown4: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown4)
        val colors = arrayOf(
            "RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "PINK",
            "BROWN", "BLACK", "WHITE", "GRAY", "GOLD", "SILVER", "MULTICOLOR"
        )
        dropdown4.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colors))
        dropdown4.setOnItemClickListener { _, _, position, _ ->
            selectedColor = colors[position]
            if (isPopularMode) exitPopularMode()
        }
    }

    private fun setupSearchButton() {
        searchButton.setOnClickListener {
            if (isPopularMode) {
                exitPopularMode()
            }
            searchLogos()
        }
    }

    private fun setupPopularButton() {
        popularButton.setOnClickListener {
            loadPopularLogos()
        }
    }

    private fun loadPopularLogos() {
        // Clear any selected filters when showing popular
        clearFilters()

        showLoading(true)
        isPopularMode = true

        // Call the top logos endpoint with limit (e.g., 20 logos)
        RetrofitClient.instance.getTopLogos(limit = 20).enqueue(object : Callback<LogoSearchResponse> {

            override fun onResponse(
                call: Call<LogoSearchResponse>,
                response: Response<LogoSearchResponse>
            ) {
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val logos = response.body()!!.logos

                    if (logos.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE

                        // Get current favorite IDs
                        val favoriteIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()

                        // Update adapter with showCount = true and the popular logos
                        networkAdapter.setShowCount(true, logos)
                        networkAdapter.updateFavoriteIds(favoriteIds)

                        Toast.makeText(
                            requireContext(),
                            "Top ${logos.size} popular logos",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No popular logos found",
                            Toast.LENGTH_SHORT
                        ).show()
                        exitPopularMode()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        requireContext(),
                        "Failed to load popular logos: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                    exitPopularMode()
                }
            }

            override fun onFailure(call: Call<LogoSearchResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                exitPopularMode()
            }
        })
    }

    private fun clearFilters() {
        selectedCategory = null
        selectedShape = null
        selectedStyle = null
        selectedColor = null

        // Clear dropdown texts
        view?.let { view ->
            val dropdown1: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown1)
            val dropdown2: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown2)
            val dropdown3: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown3)
            val dropdown4: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown4)

            dropdown1.setText("")
            dropdown2.setText("")
            dropdown3.setText("")
            dropdown4.setText("")
        }
    }

    private fun exitPopularMode() {
        isPopularMode = false
        // Clear the recycler view
        recyclerView.visibility = View.GONE
        networkAdapter.setShowCount(false, emptyList())
    }

    private fun searchLogos() {
        showLoading(true)

        // If in popular mode, exit it
        if (isPopularMode) {
            isPopularMode = false
        }

        // Use the appropriate search method based on whether color is selected
        val call = if (selectedColor != null) {
            RetrofitClient.instance.searchLogosWithColor(
                category = selectedCategory,
                shape = selectedShape,
                colors = selectedColor,
                style = selectedStyle
            )
        } else {
            RetrofitClient.instance.searchLogos(
                category = selectedCategory,
                shape = selectedShape,
                style = selectedStyle
            )
        }

        call.enqueue(object : Callback<LogoSearchResponse> {

            override fun onResponse(
                call: Call<LogoSearchResponse>,
                response: Response<LogoSearchResponse>
            ) {
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val logos = response.body()!!.logos

                    if (logos.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE

                        val favoriteIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()

                        // FIX: Set showCount = true to display like counts in search results
                        networkAdapter.setShowCount(true, logos)
                        networkAdapter.updateFavoriteIds(favoriteIds)

                        Toast.makeText(
                            requireContext(),
                            "${logos.size} logo(s) found",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No logos found with selected criteria",
                            Toast.LENGTH_SHORT
                        ).show()
                        recyclerView.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Search failed: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<LogoSearchResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        searchButton.isEnabled = !show
        popularButton.isEnabled = !show
        searchButton.text = if (show) "Loading..." else "Search"
        popularButton.text = if (show) "Loading..." else "Popular"
    }
}