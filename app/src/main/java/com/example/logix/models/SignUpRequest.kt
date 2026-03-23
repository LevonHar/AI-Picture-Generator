package com.example.logix.models

data class SignUpRequest(
    val userName: String,
    val email: String,
    val password: String,
    val repeatPassword: String
)