package com.example.logix.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.logix.EditLogoActivity
import com.example.logix.R
import com.example.logix.models.EditedLogoEntry
import java.io.File

class EditedLogoAdapter(
    private var entries: List<EditedLogoEntry>,
    private val onRemove: (EditedLogoEntry) -> Unit
) : RecyclerView.Adapter<EditedLogoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.logoImage)
        val menu: ImageView = view.findViewById(R.id.menuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val file = File(entry.savedImagePath)

        if (file.exists()) {
            Glide.with(holder.itemView.context)
                .load(file)
                .into(holder.image)
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Click image → open EditLogoActivity re-loaded with the original source
        holder.image.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditLogoActivity::class.java).apply {
                if (entry.isNetworkLogo) {
                    // Pass the network URL so EditLogoActivity loads it via loadLogoFromUrl()
                    putExtra("logo_image_url", entry.sourceId)
                } else {
                    // Pass the resource ID (stored as string, convert back to Int)
                    val resourceId = entry.sourceId.toIntOrNull() ?: 0
                    if (resourceId != 0) {
                        putExtra("logo", resourceId)
                    } else {
                        Toast.makeText(context, "Cannot open this logo for editing", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                // Also pass the saved edited image path so EditLogoActivity can
                // pre-load the already-edited version instead of the original
                putExtra("edited_logo_path", entry.savedImagePath)
                putExtra("source_id", entry.sourceId)
            }
            context.startActivity(intent)
        }

        // Long-press or menu icon → remove from edited list
        holder.menu.setOnClickListener {
            onRemove(entry)
            Toast.makeText(holder.itemView.context, "Removed from Edited Logos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<EditedLogoEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}