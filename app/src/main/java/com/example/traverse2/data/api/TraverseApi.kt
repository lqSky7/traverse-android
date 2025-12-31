package com.example.traverse2.data.api

import com.example.traverse2.data.model.AuthResponse
import com.example.traverse2.data.model.LoginRequest
import com.example.traverse2.data.model.RegisterRequest
import com.example.traverse2.data.model.User
import com.example.traverse2.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TraverseApi {
    
    // ========== AUTH ==========
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
    
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<User>
    
    // ========== USERS ==========
    
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): Response<UserProfile>
    
    @PUT("users/me")
    suspend fun updateProfile(@Body updates: Map<String, String>): Response<User>
    
    @DELETE("users/me")
    suspend fun deleteAccount(): Response<Unit>
    
    // ========== SOLVES ==========
    
    @GET("solves/me")
    suspend fun getMySolves(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<SolvesResponse>
    
    @GET("solves/me/stats")
    suspend fun getMyStats(): Response<StatsResponse>
    
    // ========== FRIENDS ==========
    
    @GET("friends")
    suspend fun getFriends(): Response<List<FriendResponse>>
    
    @GET("friends/requests")
    suspend fun getFriendRequests(): Response<FriendRequestsResponse>
    
    @POST("friends/request/{username}")
    suspend fun sendFriendRequest(@Path("username") username: String): Response<Unit>
    
    @POST("friends/accept/{requestId}")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: Int): Response<Unit>
    
    @POST("friends/reject/{requestId}")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: Int): Response<Unit>
    
    @DELETE("friends/{friendshipId}")
    suspend fun removeFriend(@Path("friendshipId") friendshipId: Int): Response<Unit>
    
    // ========== SUBMISSIONS ==========
    
    @POST("submissions")
    suspend fun submitSolution(@Body submission: SubmissionRequest): Response<SubmissionResponse>
}

// Additional response models

@kotlinx.serialization.Serializable
data class SolvesResponse(
    val solves: List<Solve>,
    val pagination: Pagination
)

@kotlinx.serialization.Serializable
data class Solve(
    val id: Int,
    val problem: Problem,
    val xpAwarded: Int,
    val solvedAt: String
)

@kotlinx.serialization.Serializable
data class Problem(
    val id: Int,
    val platform: String,
    val slug: String,
    val title: String? = null,
    val difficulty: String? = null,
    val url: String? = null
)

@kotlinx.serialization.Serializable
data class Pagination(
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val totalCount: Int
)

@kotlinx.serialization.Serializable
data class StatsResponse(
    val totalSolved: Int,
    val totalXp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val byDifficulty: DifficultyStats,
    val byPlatform: Map<String, Int>
)

@kotlinx.serialization.Serializable
data class DifficultyStats(
    val easy: Int = 0,
    val medium: Int = 0,
    val hard: Int = 0
)

@kotlinx.serialization.Serializable
data class FriendResponse(
    val friendshipId: Int,
    val friend: UserProfile,
    val since: String
)

@kotlinx.serialization.Serializable
data class FriendRequestsResponse(
    val received: List<FriendRequestItem>,
    val sent: List<FriendRequestItem>
)

@kotlinx.serialization.Serializable
data class FriendRequestItem(
    val id: Int,
    val from: UserProfile? = null,
    val to: UserProfile? = null,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class SubmissionRequest(
    val platform: String,
    val problemSlug: String,
    val problemTitle: String? = null,
    val problemUrl: String? = null,
    val difficulty: String? = null,
    val language: String,
    val accepted: Boolean,
    val idempotencyKey: String
)

@kotlinx.serialization.Serializable
data class SubmissionResponse(
    val submission: SubmissionResult,
    val solve: SolveResult? = null
)

@kotlinx.serialization.Serializable
data class SubmissionResult(
    val id: Int,
    val accepted: Boolean
)

@kotlinx.serialization.Serializable
data class SolveResult(
    val id: Int,
    val xpAwarded: Int,
    val isNewSolve: Boolean
)
