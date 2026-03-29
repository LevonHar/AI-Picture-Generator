package com.example.logix.models

import com.google.gson.annotations.SerializedName

data class LogoSearchResponse(
    @SerializedName("logos") val logos: List<LogoItem>,
    @SerializedName("totalResults") val totalResults: Int
)

data class LogoItem(
    @SerializedName("id") val id: Long,
    @SerializedName("category") val category: String,
    @SerializedName("shape") val shape: String,
    @SerializedName("style") val style: String,
    @SerializedName("colors") val colors: List<String>,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("svgUrl") val svgUrl: String?,
    @SerializedName("likeCount") val likeCount: Int
)