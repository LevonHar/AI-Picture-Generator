package com.example.logix.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.logix.models.FontOption

class FontAdapter(
    context: Context,
    private val fonts: List<FontOption>
) : ArrayAdapter<FontOption>(context, android.R.layout.simple_dropdown_item_1line, fonts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView as? TextView ?: createTextView(parent)
        val font = getItem(position)

        // Set the text to just the font name
        view.text = font?.name ?: "Default"

        // Apply the font to the dropdown item
        font?.let {
            if (it.fontResource != null) {
                ResourcesCompat.getFont(context, it.fontResource)?.let { typeface ->
                    view.typeface = typeface
                }
            } else if (it.typeface != null) {
                view.typeface = it.typeface
            }
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView as? TextView ?: createTextView(parent)
        val font = getItem(position)

        // Set the text to just the font name
        view.text = font?.name ?: "Default"

        // Apply the font to the dropdown item
        font?.let {
            if (it.fontResource != null) {
                ResourcesCompat.getFont(context, it.fontResource)?.let { typeface ->
                    view.typeface = typeface
                }
            } else if (it.typeface != null) {
                view.typeface = it.typeface
            }
        }

        return view
    }

    private fun createTextView(parent: ViewGroup): TextView {
        return LayoutInflater.from(context).inflate(
            android.R.layout.simple_dropdown_item_1line,
            parent,
            false
        ) as TextView
    }

    // Override getItem to return the FontOption
    override fun getItem(position: Int): FontOption? {
        return if (position >= 0 && position < fonts.size) {
            fonts[position]
        } else {
            null
        }
    }

    // Override getFilter to ensure proper filtering
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = fonts
                results.count = fonts.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}