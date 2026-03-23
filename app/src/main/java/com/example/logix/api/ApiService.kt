package com.example.logix.api

import com.example.logix.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("/api/public/user/signUp")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>

    @PUT("/api/public/user/verify")
    fun verify(@Body request: VerifyRequest): Call<VerifyResponse>

    @POST("/api/public/user/resend")
    fun resendVerification(@Query("email") email: String): Call<Void>

    // Based on your Swagger comment, the login might be under file-access-controller
    // Update this path according to your Swagger UI
    @POST("/account/auth")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Alternative if it's under file-access-controller:
    // @POST("/api/public/file-access-controller/login")
    // fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/api/public/user/forgotPassword")
    fun forgotPassword(@Query("email") email: String): Call<Void>
}