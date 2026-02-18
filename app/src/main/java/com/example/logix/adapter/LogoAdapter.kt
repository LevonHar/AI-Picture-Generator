package com.example.logix.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.logix.EditLogoActivity
import com.example.logix.R
import com.example.logix.viewmodel.FavoriteViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class LogoAdapter(
    private val logos: List<Int>,
    private val favoriteViewModel: FavoriteViewModel,
    private val isFavoriteList: Boolean = false // <-- new parameter
) : RecyclerView.Adapter<LogoAdapter.LogoViewHolder>() {

    class LogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.logoImage)
        val menu: ImageView = itemView.findViewById(R.id.menuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logo, parent, false)
        return LogoViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogoViewHolder, position: Int) {
        val logo = logos[position]
        holder.image.setImageResource(logo)

        // Click Logo → Open Edit Activity
        holder.image.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditLogoActivity::class.java)
            intent.putExtra("logo", logo)
            holder.itemView.context.startActivity(intent)
        }

        // Click Menu → Bottom Sheet
        holder.menu.setOnClickListener {
            showBottomSheet(holder.itemView, position)
        }
    }

    override fun getItemCount() = logos.size

    @SuppressLint("MissingInflatedId")
    private fun showBottomSheet(view: View, position: Int) {
        val dialog = BottomSheetDialog(view.context)
        val sheetView = LayoutInflater.from(view.context)
            .inflate(R.layout.bottom_sheet_logo, null)
        dialog.setContentView(sheetView)

        val like = sheetView.findViewById<TextView>(R.id.like)
        val info = sheetView.findViewById<TextView>(R.id.txtInfo)

        if (isFavoriteList) {
            // In favorites → "Delete from Favorites"
            like.text = "\uD83D\uDDD1 Remove from Favorites"
            like.setOnClickListener {
                favoriteViewModel.removeFavorite(logos[position])
                Toast.makeText(view.context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        } else {
            // Normal mode → "Like"
            like.text = "\u2764 Add to Favorites"
            like.setOnClickListener {
                favoriteViewModel.addFavorite(logos[position])
                Toast.makeText(view.context, "Added to Favorites", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        info.setOnClickListener {
            Toast.makeText(view.context, "Info Clicked", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}

