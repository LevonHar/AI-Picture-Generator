package com.example.logix.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.logix.EditLogoActivity
import com.example.logix.R
import com.example.logix.viewmodel.FavoriteViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.InputStream

class LogoAdapter(
    private val logos: List<Int>,
    private val favoriteViewModel: FavoriteViewModel,
    private val isFavoriteList: Boolean = false
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
        val logoResourceId = logos[position]

        // Load SVG from raw resource
        loadSvgFromRaw(holder.itemView.context, logoResourceId, holder.image)

        // Click Logo → Open Edit Activity
        holder.image.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditLogoActivity::class.java)
            // FIXED: Using "logo" as the key to match EditLogoActivity
            intent.putExtra("logo", logoResourceId)
            intent.putExtra("logo_position", position)
            holder.itemView.context.startActivity(intent)
        }

        // Click Menu → Bottom Sheet
        holder.menu.setOnClickListener {
            showBottomSheet(holder.itemView, position, logoResourceId)
        }
    }

    private fun loadSvgFromRaw(context: android.content.Context, rawResourceId: Int, imageView: ImageView) {
        try {
            // Open the raw resource as InputStream
            val inputStream: InputStream = context.resources.openRawResource(rawResourceId)

            // Parse SVG from InputStream
            val svg = SVG.getFromInputStream(inputStream)

            // Create a PictureDrawable from the SVG
            val pictureDrawable = PictureDrawable(svg.renderToPicture())

            // Set the drawable to ImageView
            imageView.setImageDrawable(pictureDrawable)

            // Close the input stream
            inputStream.close()

        } catch (e: SVGParseException) {
            e.printStackTrace()
            // Fallback image if SVG parsing fails
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback image for any other error
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount() = logos.size

    @SuppressLint("MissingInflatedId")
    private fun showBottomSheet(view: View, position: Int, logoResourceId: Int) {
        val dialog = BottomSheetDialog(view.context)
        val sheetView = LayoutInflater.from(view.context)
            .inflate(R.layout.bottom_sheet_logo, null)
        dialog.setContentView(sheetView)

        val like = sheetView.findViewById<TextView>(R.id.like)
        val info = sheetView.findViewById<TextView>(R.id.txtInfo)

        if (isFavoriteList) {
            like.text = "🗑 Remove from Favorites"
            like.setOnClickListener {
                favoriteViewModel.removeFavorite(logoResourceId)
                Toast.makeText(view.context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        } else {
            like.text = "❤️ Add to Favorites"
            like.setOnClickListener {
                favoriteViewModel.addFavorite(logoResourceId)
                Toast.makeText(view.context, "Added to Favorites", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        info.setOnClickListener {
            Toast.makeText(view.context, "Logo Info", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}