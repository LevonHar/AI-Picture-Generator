package com.example.logix.models

data class VerifyRequest(
    val email: String,
    val pin: String
)