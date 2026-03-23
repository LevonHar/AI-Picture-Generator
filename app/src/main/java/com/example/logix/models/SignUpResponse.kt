package com.example.logix.models

data class SignUpResponse(
    val userId: Long,
    val userName: String,
    val email: String,
    val verifyMail: Boolean,
    val role: String
)