package com.example.logix.models

data class LoginResponse(
    val userId: Long,
    val userName: String,
    val email: String,
    val token: String,
    val role: String
)