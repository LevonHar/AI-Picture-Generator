package com.example.logix.models

data class EditedLogoEntry(
    val sourceId: String,        // resource ID (as string) or network URL
    val isNetworkLogo: Boolean,
    val savedImagePath: String,  // absolute path to the saved PNG in internal storage
    val savedAt: Long = System.currentTimeMillis()
)