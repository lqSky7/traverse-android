package com.example.traverse2.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val message: String,
    val user: User
)

@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val timezone: String? = null,
    val visibility: String = "public",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val createdAt: String? = null
)

@Serializable
data class UserProfile(
    val id: Int,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val visibility: String = "public",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class MessageResponse(
    val message: String
)
