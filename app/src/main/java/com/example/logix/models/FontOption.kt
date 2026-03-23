package com.example.logix.models

import android.graphics.Typeface

data class FontOption(
    val name: String,
    val typeface: Typeface? = null,
    val fontResource: Int? = null,
    val isSystemFont: Boolean = true
) {
    // Override toString to return just the name
    override fun toString(): String {
        return name
    }
}