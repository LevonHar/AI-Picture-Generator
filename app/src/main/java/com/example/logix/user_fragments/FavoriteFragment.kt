package com.example.logix.user_fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.logix.R
import com.example.logix.adapter.LogoAdapter
import com.example.logix.viewmodel.FavoriteViewModel

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val favoriteViewModel: FavoriteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite, container, false)
        recyclerView = view.findViewById(R.id.favRecycler)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        favoriteViewModel.favoriteLogos.observe(viewLifecycleOwner) { favorites ->
            recyclerView.adapter = LogoAdapter(favorites, favoriteViewModel, isFavoriteList = true)
        }


        return view
    }
}
