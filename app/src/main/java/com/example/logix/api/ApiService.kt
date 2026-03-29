package com.example.logix.api

import com.example.logix.models.LogoSearchResponse
import com.example.logix.models.LoginRequest
import com.example.logix.models.LoginResponse
import com.example.logix.models.SignUpRequest
import com.example.logix.models.SignUpResponse
import com.example.logix.models.VerifyRequest
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Query

interface ApiService {

    @POST("/api/public/user/signUp")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>

    @PUT("/api/public/user/verify")
    fun verify(@Body request: VerifyRequest): Call<Void>

    @POST("/api/public/user/resend")
    fun resendVerification(@Query("email") email: String): Call<Void>

    @POST("/account/auth")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/api/public/user/forgotPassword")
    fun forgotPassword(@Query("email") email: String): Call<Void>

    // Search without colors (simpler, avoids list encoding issues)
    @GET("/api/public/logo/search/priority")
    fun searchLogos(
        @Query("category") category: String?,
        @Query("shape") shape: String?,
        @Query("style") style: String?
    ): Call<LogoSearchResponse>

    // Search with colors as separate call if needed
    @GET("/api/public/logo/search/priority")
    fun searchLogosWithColor(
        @Query("category") category: String?,
        @Query("shape") shape: String?,
        @Query("colors") colors: String?,   // Send as single string
        @Query("style") style: String?
    ): Call<LogoSearchResponse>
}