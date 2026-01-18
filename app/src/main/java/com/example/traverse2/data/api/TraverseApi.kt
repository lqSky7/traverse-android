package com.example.traverse2.data.api

import com.example.traverse2.data.model.AuthResponse
import com.example.traverse2.data.model.LoginRequest
import com.example.traverse2.data.model.MessageResponse
import com.example.traverse2.data.model.RegisterRequest
import com.example.traverse2.data.model.User
import com.example.traverse2.data.model.UserProfile
import com.example.traverse2.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @PATCH("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UpdateProfileResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    @DELETE("auth/account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<MessageResponse>

    // ========== USERS ==========
    
    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): Response<UserProfile>
    
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
    suspend fun getFriends(): Response<FriendsListResponse>

    // Friend Requests
    @POST("friends/request")
    suspend fun sendFriendRequest(@Body request: FriendRequestBody): Response<FriendRequestResponse>

    @GET("friends/requests/received")
    suspend fun getReceivedFriendRequests(): Response<List<ReceivedFriendRequest>>

    @GET("friends/requests/sent")
    suspend fun getSentFriendRequests(): Response<List<SentFriendRequest>>

    @POST("friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: Int): Response<AcceptFriendResponse>

    @POST("friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: Int): Response<MessageResponse>

    @DELETE("friends/requests/{requestId}")
    suspend fun cancelFriendRequest(@Path("requestId") requestId: Int): Response<MessageResponse>

    @DELETE("friends/{username}")
    suspend fun removeFriend(@Path("username") username: String): Response<MessageResponse>

    // Friend Profile Endpoints
    @GET("friends/{username}/solves")
    suspend fun getFriendSolves(
        @Path("username") username: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<FriendSolvesResponse>

    @GET("friends/{username}/stats")
    suspend fun getFriendStats(@Path("username") username: String): Response<FriendStatsResponse>

    @GET("friends/{username}/achievements")
    suspend fun getFriendAchievements(@Path("username") username: String): Response<FriendAchievementsResponse>

    // ========== FRIEND STREAKS ==========

    @GET("friend-streaks")
    suspend fun getFriendStreaks(): Response<List<FriendStreakItem>>

    @POST("friend-streaks/request")
    suspend fun sendFriendStreakRequest(@Body request: FriendStreakRequestBody): Response<FriendStreakRequestResponse>

    @GET("friend-streaks/requests/received")
    suspend fun getReceivedStreakRequests(): Response<List<ReceivedStreakRequest>>

    @GET("friend-streaks/requests/sent")
    suspend fun getSentStreakRequests(): Response<List<SentStreakRequest>>

    @POST("friend-streaks/requests/{requestId}/accept")
    suspend fun acceptStreakRequest(@Path("requestId") requestId: Int): Response<AcceptStreakResponse>

    @POST("friend-streaks/requests/{requestId}/reject")
    suspend fun rejectStreakRequest(@Path("requestId") requestId: Int): Response<MessageResponse>

    @DELETE("friend-streaks/requests/{requestId}")
    suspend fun cancelStreakRequest(@Path("requestId") requestId: Int): Response<MessageResponse>

    @DELETE("friend-streaks/{username}")
    suspend fun deleteFriendStreak(@Path("username") username: String): Response<MessageResponse>

    // ========== USER SEARCH ==========

    @GET("users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): Response<UsersSearchResponse>
    
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
    
    // ========== REVISIONS ==========
    
    @GET("revisions")
    suspend fun getRevisions(
        @Query("upcoming") upcoming: Boolean? = null,
        @Query("overdue") overdue: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("type") type: String = "normal"
    ): Response<RevisionsResponse>
    
    @GET("revisions/grouped")
    suspend fun getRevisionsGrouped(
        @Query("includeCompleted") includeCompleted: Boolean = false,
        @Query("type") type: String = "normal"
    ): Response<RevisionsGroupedResponse>
    
    @GET("revisions/stats")
    suspend fun getRevisionStats(
        @Query("type") type: String = "normal"
    ): Response<RevisionStatsResponse>
    
    @POST("revisions/{id}/complete")
    suspend fun completeRevision(
        @Path("id") revisionId: Int
    ): Response<RevisionCompleteResponse>
    
    @POST("revisions/{id}/attempt")
    suspend fun recordRevisionAttempt(
        @Path("id") revisionId: Int,
        @Body attemptData: RevisionAttemptRequest
    ): Response<RevisionAttemptResponse>
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

// ========== FRIENDS MODELS ==========

@kotlinx.serialization.Serializable
data class FriendsListResponse(
    val friends: List<FriendItem>
)

@kotlinx.serialization.Serializable
data class FriendItem(
    val friendshipId: String,
    val friendedAt: String? = null,
    val id: Int,
    val username: String,
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val visibility: String = "public"
)

@kotlinx.serialization.Serializable
data class FriendRequestBody(
    val username: String
)

@kotlinx.serialization.Serializable
data class FriendRequestResponse(
    val friendRequest: FriendRequestDetail
)

@kotlinx.serialization.Serializable
data class FriendRequestDetail(
    val id: Int,
    val requesterId: Int,
    val requestedId: Int,
    val status: String,
    val createdAt: String,
    val requester: FriendUserInfo,
    val requested: FriendUserInfo
)

@kotlinx.serialization.Serializable
data class FriendUserInfo(
    val id: Int,
    val username: String,
    val currentStreak: Int = 0,
    val totalXp: Int = 0
)

@kotlinx.serialization.Serializable
data class ReceivedFriendRequest(
    val id: Int,
    val requester: FriendUserInfo,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class SentFriendRequest(
    val id: Int,
    val addressee: FriendUserInfo,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class AcceptFriendResponse(
    val message: String,
    val friendRequest: FriendRequestStatusUpdate
)

@kotlinx.serialization.Serializable
data class FriendRequestStatusUpdate(
    val id: Int,
    val status: String
)

// Friend Profile Models
@kotlinx.serialization.Serializable
data class FriendSolvesResponse(
    val solves: List<FriendSolveItem>,
    val pagination: PaginationInfo
)

@kotlinx.serialization.Serializable
data class FriendSolveItem(
    val id: Int,
    val problem: Problem,
    val submission: FriendSubmissionInfo? = null,
    val highlights: List<FriendHighlight> = emptyList(),
    val xpAwarded: Int,
    val solvedAt: String
)

@kotlinx.serialization.Serializable
data class FriendSubmissionInfo(
    val language: String,
    val happenedAt: String
)

@kotlinx.serialization.Serializable
data class FriendHighlight(
    val id: Int,
    val content: String,
    val notes: String? = null,
    val tags: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class FriendStatsResponse(
    val currentStreak: Int,
    val totalXp: Int,
    val totalSolves: Int,
    val totalSubmissions: Int,
    val totalStreakDays: Int,
    val byDifficulty: Map<String, Int> = emptyMap()
)

@kotlinx.serialization.Serializable
data class FriendAchievementsResponse(
    val achievements: List<FriendAchievementItem>
)

@kotlinx.serialization.Serializable
data class FriendAchievementItem(
    val id: Int,
    val key: String,
    val name: String,
    val description: String,
    val category: String,
    val unlockedAt: String
)

// ========== FRIEND STREAKS MODELS ==========

@kotlinx.serialization.Serializable
data class FriendStreakItem(
    val friend: FriendUserInfo,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastIncrementDate: String? = null,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class FriendStreakRequestBody(
    val username: String
)

@kotlinx.serialization.Serializable
data class FriendStreakRequestResponse(
    val streakRequest: StreakRequestDetail
)

@kotlinx.serialization.Serializable
data class StreakRequestDetail(
    val id: Int,
    val requesterId: Int,
    val requestedId: Int,
    val status: String,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class ReceivedStreakRequest(
    val id: Int,
    val requester: FriendUserInfo,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class SentStreakRequest(
    val id: Int,
    val addressee: FriendUserInfo,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class AcceptStreakResponse(
    val message: String,
    val friendStreak: FriendStreakCreated
)

@kotlinx.serialization.Serializable
data class FriendStreakCreated(
    val userId1: Int,
    val userId2: Int,
    val currentStreak: Int,
    val longestStreak: Int
)

// ========== USER SEARCH MODELS ==========

@kotlinx.serialization.Serializable
data class UsersSearchResponse(
    val users: List<UserSearchResult>
)

@kotlinx.serialization.Serializable
data class UserSearchResult(
    val id: Int,
    val username: String,
    val currentStreak: Int = 0,
    val totalXp: Int = 0
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

// ========== REVISIONS MODELS ==========

@kotlinx.serialization.Serializable
data class RevisionsResponse(
    val revisions: List<RevisionItem>,
    val pagination: PaginationInfo
)

@kotlinx.serialization.Serializable
data class RevisionItem(
    val id: Int,
    val revisionNumber: Int,
    val scheduledFor: String,
    val completedAt: String? = null,
    val type: String,
    val problem: Problem,
    val solve: RevisionSolve
)

@kotlinx.serialization.Serializable
data class RevisionSolve(
    val id: Int,
    val xpAwarded: Int,
    val solvedAt: String
)

@kotlinx.serialization.Serializable
data class RevisionsGroupedResponse(
    val groups: List<RevisionGroup>
)

@kotlinx.serialization.Serializable
data class RevisionGroup(
    val date: String,
    val revisions: List<RevisionItem>,
    val count: Int
)

@kotlinx.serialization.Serializable
data class RevisionStatsResponse(
    val total: Int,
    val completed: Int,
    val overdue: Int,
    val dueToday: Int,
    val completionRate: Int
)

@kotlinx.serialization.Serializable
data class RevisionCompleteResponse(
    val message: String,
    val revision: RevisionItem
)

@kotlinx.serialization.Serializable
data class RevisionAttemptRequest(
    val outcome: Int, // 0 = failed, 1 = success
    val numTries: Int,
    val timeSpentMinutes: Int
)

@kotlinx.serialization.Serializable
data class RevisionAttemptResponse(
    val attempt: RevisionAttemptData,
    val nextRevision: RevisionItem?,
    val prediction: MLPrediction
)

@kotlinx.serialization.Serializable
data class RevisionAttemptData(
    val id: Int,
    val attemptNumber: Int,
    val outcome: Int,
    val numTries: Int,
    val timeSpentMinutes: Int
)

@kotlinx.serialization.Serializable
data class MLPrediction(
    val next_review_interval_days: Float,
    val confidence: String
)

// ========== PROFILE UPDATE MODELS ==========

@kotlinx.serialization.Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    val timezone: String? = null,
    val visibility: String? = null
)

@kotlinx.serialization.Serializable
data class UpdateProfileResponse(
    val message: String,
    val user: com.example.traverse2.data.model.User
)

@kotlinx.serialization.Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@kotlinx.serialization.Serializable
data class DeleteAccountRequest(
    val password: String
)
