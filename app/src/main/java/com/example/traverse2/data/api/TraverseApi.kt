package com.example.traverse2.data.api

import com.example.traverse2.data.model.AuthResponse
import com.example.traverse2.data.model.LoginRequest
import com.example.traverse2.data.model.RegisterRequest
import com.example.traverse2.data.model.User
import com.example.traverse2.data.model.UserProfile
import com.example.traverse2.data.model.UserResponse
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
    suspend fun getCurrentUser(): Response<UserResponse>
    
    // ========== USERS ==========
    
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): Response<UserProfile>
    
    @PUT("users/me")
    suspend fun updateProfile(@Body updates: Map<String, String>): Response<User>
    
    @DELETE("users/me")
    suspend fun deleteAccount(): Response<Unit>
    
    // ========== SOLVES ==========
    
    @GET("solves")
    suspend fun getMySolves(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<SolvesResponse>
    
    @GET("solves/stats/summary")
    suspend fun getSolveStats(): Response<SolveStatsResponse>
    
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
    
    // ========== ACHIEVEMENTS ==========
    
    @GET("achievements")
    suspend fun getAchievements(): Response<AchievementsResponse>
    
    // ========== SUBMISSIONS ==========
    
    @POST("submissions")
    suspend fun submitSolution(@Body submission: SubmissionRequest): Response<SubmissionResponse>
    
    @GET("submissions")
    suspend fun getSubmissions(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("outcome") outcome: String? = null
    ): Response<SubmissionsResponse>
    
    @GET("submissions/stats/summary")
    suspend fun getSubmissionStats(): Response<SubmissionStatsResponse>
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
    val id: Int? = null,
    val platform: String,
    val slug: String,
    val title: String? = null,
    val difficulty: String? = null,
    val url: String? = null
)

@kotlinx.serialization.Serializable
data class Pagination(
    val total: Int,
    val limit: Int,
    val offset: Int
)

@kotlinx.serialization.Serializable
data class SolveStatsResponse(
    val stats: SolveStats
)

@kotlinx.serialization.Serializable
data class SolveStats(
    val totalSolves: Int,
    val totalXp: Int,
    val totalStreakDays: Int,
    val byDifficulty: Map<String, Int>,
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

@kotlinx.serialization.Serializable
data class AchievementsResponse(
    val achievements: List<Achievement>
)

@kotlinx.serialization.Serializable
data class Achievement(
    val id: Int,
    val key: String,
    val name: String,
    val description: String,
    val icon: String? = null,
    val category: String,
    val unlocked: Boolean,
    val unlockedAt: String? = null
)

@kotlinx.serialization.Serializable
data class SubmissionsResponse(
    val submissions: List<SubmissionItem>,
    val pagination: PaginationInfo
)

@kotlinx.serialization.Serializable
data class SubmissionItem(
    val id: Int,
    val problem: Problem,
    val language: String,
    val outcome: String,
    val happenedAt: String,
    val timeTaken: Int? = null,
    val numberOfTries: Int? = null
)

@kotlinx.serialization.Serializable
data class PaginationInfo(
    val total: Int,
    val limit: Int,
    val offset: Int
)

@kotlinx.serialization.Serializable
data class SubmissionStatsResponse(
    val stats: SubmissionStats
)

@kotlinx.serialization.Serializable
data class SubmissionStats(
    val total: Int,
    val accepted: Int,
    val failed: Int,
    val acceptanceRate: String,
    val languageBreakdown: List<LanguageStat>
)

@kotlinx.serialization.Serializable
data class LanguageStat(
    val language: String,
    val count: Int
)
