package com.example.logix.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.logix.EditLogoActivity
import com.example.logix.R
import com.example.logix.models.LogoItem
import com.google.android.material.bottomsheet.BottomSheetDialog

class LogoNetworkAdapter(
    private var logos: List<LogoItem>,
    private val baseUrl: String,
    private val onFavoriteToggle: (LogoItem, Boolean) -> Unit,
    private var favoriteIds: Set<Long> = emptySet()
) : RecyclerView.Adapter<LogoNetworkAdapter.LogoViewHolder>() {

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

        // Build full image URL — backend serves files at /api/uploads/{filename}
        val imageUrl = "$baseUrl/api/uploads/${logo.imageUrl}"
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.image)

        holder.image.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditLogoActivity::class.java)
            intent.putExtra("logo_id", logo.id)
            intent.putExtra("logo_image_url", imageUrl)
            // Also pass the entire logo object if EditLogoActivity needs more data
            intent.putExtra("logo_category", logo.category)
            intent.putExtra("logo_style", logo.style)
            intent.putExtra("logo_like_count", logo.likeCount)
            holder.itemView.context.startActivity(intent)
        }

        holder.menu.setOnClickListener {
            showBottomSheet(holder.itemView, logo)
        }
    }

    override fun getItemCount() = logos.size

    fun updateLogos(newLogos: List<LogoItem>) {
        logos = newLogos
        notifyDataSetChanged()
    }

    fun updateLogosWithFavorites(newLogos: List<LogoItem>, newFavoriteIds: Set<Long>) {
        logos = newLogos
        favoriteIds = newFavoriteIds
        notifyDataSetChanged()
    }

    fun updateFavoriteIds(newFavoriteIds: Set<Long>) {
        favoriteIds = newFavoriteIds
        // Only refresh visible items to update the menu text
        notifyItemRangeChanged(0, itemCount)
    }

    private fun showBottomSheet(view: View, logo: LogoItem) {
        val dialog = BottomSheetDialog(view.context)
        val sheetView = LayoutInflater.from(view.context)
            .inflate(R.layout.bottom_sheet_logo, null)
        dialog.setContentView(sheetView)

        val like = sheetView.findViewById<TextView>(R.id.like)
        val info = sheetView.findViewById<TextView>(R.id.txtInfo)

        val isFav = favoriteIds.contains(logo.id)
        like.text = if (isFav) "🗑 Remove from Favorites" else "❤️ Add to Favorites"

        like.setOnClickListener {
            // Call the toggle callback
            onFavoriteToggle(logo, !isFav)

            // Update local favoriteIds set for immediate UI feedback
            if (!isFav) {
                favoriteIds = favoriteIds.toMutableSet().apply { add(logo.id) }
            } else {
                favoriteIds = favoriteIds.toMutableSet().apply { remove(logo.id) }
            }

            Toast.makeText(
                view.context,
                if (isFav) "Removed from Favorites" else "Added to Favorites",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        info.setOnClickListener {
            // Show more detailed info
            val infoText = buildString {
                append("ID: ${logo.id}\n")
                append("Category: ${logo.category ?: "N/A"}\n")
                append("Style: ${logo.style ?: "N/A"}\n")
                append("Shape: ${logo.shape ?: "N/A"}\n")
                append("Likes: ${logo.likeCount}")
            }
            Toast.makeText(view.context, infoText, Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}