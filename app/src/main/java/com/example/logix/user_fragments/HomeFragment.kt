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
    private lateinit var networkAdapter: LogoNetworkAdapter
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    private var selectedCategory: String? = null
    private var selectedShape: String? = null
    private var selectedStyle: String? = null
    private var selectedColor: String? = null

    private val baseUrl = "http://192.168.10.48:8080"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initializeViews(view)
        setupRecyclerView()
        setupDropdownsWithListeners(view)
        setupSearchButton()

        return view
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchButton = view.findViewById(R.id.search)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Get current favorite IDs
        val favoriteIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()

        networkAdapter = LogoNetworkAdapter(
            logos = emptyList(),
            baseUrl = baseUrl,
            onFavoriteToggle = { logo, addToFav ->
                favoriteViewModel.toggleFavorite(logo, addToFav)
                // Update the adapter's favorite IDs
                val updatedFavIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()
                networkAdapter.updateFavoriteIds(updatedFavIds)
                Toast.makeText(
                    requireContext(),
                    if (addToFav) "Added to Favorites" else "Removed from Favorites",
                    Toast.LENGTH_SHORT
                ).show()
            },
            favoriteIds = favoriteIds
        )
        recyclerView.adapter = networkAdapter

        // Observe favorite changes to update UI
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
        dropdown1.setOnItemClickListener { _, _, position, _ -> selectedCategory = categories[position] }

        val dropdown2: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown2)
        val shapes = arrayOf("CIRCLE", "SQUARE", "TRIANGLE", "HEXAGON", "RECTANGLE", "ABSTRACT", "SYMBOL")
        dropdown2.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, shapes))
        dropdown2.setOnItemClickListener { _, _, position, _ -> selectedShape = shapes[position] }

        val dropdown3: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown3)
        val styles = arrayOf("MINIMAL", "MODERN", "CLASSIC", "FUTURISTIC", "PLAYFUL", "LUXURY", "RETRO", "ABSTRACT")
        dropdown3.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, styles))
        dropdown3.setOnItemClickListener { _, _, position, _ -> selectedStyle = styles[position] }

        val dropdown4: MaterialAutoCompleteTextView = view.findViewById(R.id.dropdown4)
        val colors = arrayOf(
            "RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "PINK",
            "BROWN", "BLACK", "WHITE", "GRAY", "GOLD", "SILVER"
        )
        dropdown4.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colors))
        dropdown4.setOnItemClickListener { _, _, position, _ -> selectedColor = colors[position] }
    }

    private fun setupSearchButton() {
        searchButton.setOnClickListener {
            searchLogos()
        }
    }

    private fun searchLogos() {
        showLoading(true)

        RetrofitClient.instance.searchLogos(
            category = selectedCategory,
            shape = selectedShape,
            style = selectedStyle
        ).enqueue(object : Callback<LogoSearchResponse> {

            override fun onResponse(
                call: Call<LogoSearchResponse>,
                response: Response<LogoSearchResponse>
            ) {
                showLoading(false)

                if (response.isSuccessful) {
                    val logos = response.body()?.logos ?: emptyList()
                    recyclerView.visibility = View.VISIBLE

                    // Get current favorite IDs to mark favorites
                    val favoriteIds = favoriteViewModel.favoriteNetworkLogos.value?.map { it.id }?.toSet() ?: emptySet()
                    networkAdapter.updateLogosWithFavorites(logos, favoriteIds)

                    val msg = if (logos.isEmpty()) "No logos found" else "${logos.size} logo(s) found"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
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
    }
}