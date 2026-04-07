package com.example.logix.user_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.logix.R
import com.example.logix.adapter.EditedLogoAdapter
import com.example.logix.adapter.LogoNetworkAdapter
import com.example.logix.viewmodel.EditedLogosViewModel
import com.example.logix.viewmodel.FavoriteViewModel

class FavoriteFragment : Fragment() {

    private lateinit var favRecyclerView: RecyclerView
    private lateinit var changedRecyclerView: RecyclerView
    private lateinit var favEmptyView: TextView
    private lateinit var changedEmptyView: TextView
    private lateinit var adapter: LogoNetworkAdapter
    private lateinit var editedLogoAdapter: EditedLogoAdapter

    private val favoriteViewModel: FavoriteViewModel by activityViewModels()
    private val editedLogosViewModel: EditedLogosViewModel by activityViewModels()
    private val baseUrl = "http://192.168.10.48:8080"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite, container, false)

        // --- Favorites RecyclerView ---
        favRecyclerView = view.findViewById(R.id.favRecycler)
        favEmptyView = view.findViewById(R.id.favEmptyView)
        favRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = LogoNetworkAdapter(
            logos = emptyList(),
            baseUrl = baseUrl,
            onFavoriteToggle = { logo, addToFav ->
                favoriteViewModel.toggleFavorite(logo, addToFav)
                updateFavoritesList()
                Toast.makeText(
                    requireContext(),
                    if (addToFav) "Added to Favorites" else "Removed from Favorites",
                    Toast.LENGTH_SHORT
                ).show()
            },
            favoriteIds = emptySet()
        )
        favRecyclerView.adapter = adapter

        // --- Edited Logos RecyclerView ---
        changedRecyclerView = view.findViewById(R.id.changedRecycler)
        changedEmptyView = view.findViewById(R.id.changedEmptyView)
        changedRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        editedLogoAdapter = EditedLogoAdapter(
            entries = emptyList(),
            onRemove = { entry ->
                editedLogosViewModel.removeEditedLogo(entry.sourceId)
            }
        )
        changedRecyclerView.adapter = editedLogoAdapter

        observeFavorites()
        observeEditedLogos()

        return view
    }

    private fun observeFavorites() {
        favoriteViewModel.favoriteNetworkLogos.observe(viewLifecycleOwner) {
            updateFavoritesList()
        }
    }

    private fun observeEditedLogos() {
        editedLogosViewModel.editedLogos.observe(viewLifecycleOwner) { entries ->
            editedLogoAdapter.updateEntries(entries)
            updateChangedEmptyView(entries.isEmpty())
        }
    }

    private fun updateFavoritesList() {
        val favorites = favoriteViewModel.favoriteNetworkLogos.value ?: emptyList()
        val favoriteIds = favorites.map { it.id }.toSet()
        adapter.updateLogosWithFavorites(favorites, favoriteIds)
        updateFavEmptyView(favorites.isEmpty())
    }

    private fun updateFavEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            favRecyclerView.visibility = View.GONE
            favEmptyView.visibility = View.VISIBLE
        } else {
            favRecyclerView.visibility = View.VISIBLE
            favEmptyView.visibility = View.GONE
        }
    }

    private fun updateChangedEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            changedRecyclerView.visibility = View.GONE
            changedEmptyView.visibility = View.VISIBLE
        } else {
            changedRecyclerView.visibility = View.VISIBLE
            changedEmptyView.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateFavoritesList()
        editedLogosViewModel.loadEditedLogos() // refresh on return from EditLogoActivity
    }
}