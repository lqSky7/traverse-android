package com.traverse.android.data

import kotlinx.serialization.Serializable

// MARK: - Request Models

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val timezone: String
)

// MARK: - Response Models

@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String? = null,
    val timezone: String,
    val visibility: String,
    val currentStreak: Int,
    val totalXp: Int,
    val createdAt: String? = null,
    val profileImageURL: String? = null,
    // Local-only field for cached cat pic
    @kotlinx.serialization.Transient
    val localProfileImageUrl: String? = null
)

@Serializable
data class AuthResponse(
    val message: String,
    val user: User,
    val token: String? = null
)

@Serializable
data class LoginResponse(
    val message: String,
    val user: User,
    val token: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)
