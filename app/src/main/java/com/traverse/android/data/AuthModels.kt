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

@Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    val timezone: String? = null,
    val visibility: String? = null,
    val maxDailyReviews: Int? = null
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class DeleteAccountRequest(
    val password: String
)

@Serializable
data class RecoverAccountRequest(
    val username: String,
    val password: String? = null
)

// MARK: - Response Models

@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String? = null,
    val timezone: String = "UTC",
    val visibility: String = "PUBLIC",
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val maxDailyReviews: Int? = null,
    val createdAt: String? = null,
    val profileImageURL: String? = null,
    val calendarToken: String? = null,
    // Local-only field for cached avatar
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
data class UserResponse(
    val user: User
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class RecoveryResponse(
    val message: String,
    val user: User,
    val token: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)

// MARK: - Password Reset Models

@Serializable
data class PasswordResetRequest(
    val username: String
)

@Serializable
data class PasswordResetConfirmRequest(
    val username: String,
    val code: String,
    val newPassword: String
)

@Serializable
data class PasswordResetRequestResponse(
    val status: String,
    val message: String,
    val expiresInMinutes: Int? = null
)

@Serializable
data class PasswordResetConfirmResponse(
    val status: String,
    val message: String
)
