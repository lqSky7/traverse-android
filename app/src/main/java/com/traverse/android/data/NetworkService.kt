package com.traverse.android.data

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * GitHub API for checking latest releases
 */
interface GitHubApi {
    @GET("repos/lqSky7/traverse-android/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}

@kotlinx.serialization.Serializable
data class GitHubRelease(
    @kotlinx.serialization.SerialName("tag_name")
    val tagName: String,
    @kotlinx.serialization.SerialName("html_url")
    val htmlUrl: String,
    val body: String? = null
) {
    val version: String
        get() = tagName.removePrefix("v")
}

// API Base URLs
private const val TRAVERSE_API_URL = "https://traverse-backend-api.azurewebsites.net/api/"
private const val GITHUB_API_URL = "https://api.github.com/"

/**
 * Network service for Traverse API calls.
 * Uses Retrofit + OkHttp with kotlinx.serialization.
 */
interface TraverseApi {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("auth/logout")
    suspend fun logout()
    
    @GET("auth/me")
    suspend fun getCurrentUser(): UserResponse
    
    @retrofit2.http.PATCH("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UpdateProfileResponse
    
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse
    
    @retrofit2.http.HTTP(method = "DELETE", path = "auth/account", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): MessageResponse
    
    @GET("auth/me/stats")
    suspend fun getUserStats(): UserStats
    
    @GET("solves/stats/summary")
    suspend fun getSolveStats(): SolveStats
    
    @GET("solves")
    suspend fun getSolves(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SolvesResponse
    
    @GET("achievements/stats/summary")
    suspend fun getAchievementStats(): AchievementStats
    
    @GET("achievements")
    suspend fun getAllAchievements(): AllAchievementsResponse
    
    @GET("freeze/dates")
    suspend fun getFreezeDates(): FreezeDatesResponse
    
    @GET("revisions/grouped")
    suspend fun getGroupedRevisions(
        @Query("includeCompleted") includeCompleted: Boolean = false,
        @Query("type") type: String = "normal"
    ): GroupedRevisionsResponse
    
    @GET("revisions/stats")
    suspend fun getRevisionStats(
        @Query("type") type: String = "normal"
    ): RevisionStatsResponse
    
    @POST("revisions/{id}/complete")
    suspend fun completeRevision(@retrofit2.http.Path("id") id: Int): CompleteRevisionResponse
    
    @retrofit2.http.DELETE("revisions/{id}")
    suspend fun deleteRevision(@retrofit2.http.Path("id") id: Int)
    
    @POST("revisions/{id}/attempt")
    suspend fun recordRevisionAttempt(
        @retrofit2.http.Path("id") id: Int,
        @Body request: RevisionAttemptRequest
    ): RevisionAttemptResponse
    
    // MARK: - Friends API
    
    @GET("friends")
    suspend fun getFriends(): FriendsListResponse
    
    @GET("friends/requests/received")
    suspend fun getReceivedFriendRequests(): FriendRequestsResponse
    
    @GET("friends/requests/sent")
    suspend fun getSentFriendRequests(): FriendRequestsResponse
    
    @POST("friends/request")
    suspend fun sendFriendRequest(@Body request: SendFriendRequestBody): SendFriendRequestResponse
    
    @POST("friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(@retrofit2.http.Path("id") id: Int): AcceptFriendRequestResponse
    
    @POST("friends/requests/{id}/reject")
    suspend fun rejectFriendRequest(@retrofit2.http.Path("id") id: Int): FriendRequestActionResponse
    
    @retrofit2.http.DELETE("friends/requests/{id}")
    suspend fun cancelFriendRequest(@retrofit2.http.Path("id") id: Int): FriendRequestActionResponse
    
    @retrofit2.http.DELETE("friends/{username}")
    suspend fun removeFriend(@retrofit2.http.Path("username") username: String): RemoveFriendResponse
    
    // MARK: - Friend Streaks API
    
    @POST("friend-streaks/request")
    suspend fun sendFriendStreakRequest(@Body request: SendFriendStreakRequestBody): SendFriendStreakRequestResponse
    
    @GET("friend-streaks/requests/received")
    suspend fun getReceivedFriendStreakRequests(): FriendStreakRequestsResponse
    
    @GET("friend-streaks/requests/sent")
    suspend fun getSentFriendStreakRequests(): FriendStreakRequestsResponse
    
    @POST("friend-streaks/requests/{id}/accept")
    suspend fun acceptFriendStreakRequest(@retrofit2.http.Path("id") id: Int): AcceptFriendStreakRequestResponse
    
    @POST("friend-streaks/requests/{id}/reject")
    suspend fun rejectFriendStreakRequest(@retrofit2.http.Path("id") id: Int): MessageResponse
    
    @retrofit2.http.DELETE("friend-streaks/requests/{id}")
    suspend fun cancelFriendStreakRequest(@retrofit2.http.Path("id") id: Int): MessageResponse
    
    @GET("friend-streaks")
    suspend fun getFriendStreaks(): FriendStreaksResponse
    
    @retrofit2.http.DELETE("friend-streaks/{username}")
    suspend fun deleteFriendStreak(@retrofit2.http.Path("username") username: String): MessageResponse

    // MARK: - User Search & Profile
    
    @GET("users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): UsersSearchResponse
    
    @GET("users/{username}")
    suspend fun getUserProfile(@retrofit2.http.Path("username") username: String): UserProfileResponse
    
    @GET("users/{username}/stats")
    suspend fun getUserStatistics(@retrofit2.http.Path("username") username: String): UserStatisticsResponse
    
    // MARK: - Friend Profile Data
    
    @GET("friends/{username}/solves")
    suspend fun getFriendSolves(
        @retrofit2.http.Path("username") username: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): FriendSolvesResponse
    
    @GET("friends/{username}/stats")
    suspend fun getFriendStats(@retrofit2.http.Path("username") username: String): UserStatisticsResponse
    
    @GET("friends/{username}/achievements")
    suspend fun getFriendAchievements(@retrofit2.http.Path("username") username: String): FriendAchievementsResponse
    
    // MARK: - Freeze Shop API
    
    @GET("users/me/freezes")
    suspend fun getFreezeInfo(): FreezeInfoResponse
    
    @POST("users/me/freezes/purchase")
    suspend fun purchaseFreezes(@Body request: PurchaseFreezeRequest): PurchaseFreezeResponse
    
    @POST("users/{username}/freezes/gift")
    suspend fun giftFreezes(
        @retrofit2.http.Path("username") username: String,
        @Body request: GiftFreezeRequest
    ): GiftFreezeResponse
    
    // MARK: - Subscription API
    
    @GET("subscription/status")
    suspend fun getSubscriptionStatus(): SubscriptionStatusResponse
}

@kotlinx.serialization.Serializable
data class UserResponse(val user: User)

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}

class NetworkService private constructor(context: Context) {
    
    private val tokenManager = TokenManager.getInstance(context)
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = tokenManager.getToken()
        
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Cookie", "auth_token=$token")
                .build()
        } else {
            originalRequest
        }
        
        chain.proceed(newRequest)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(TRAVERSE_API_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val gitHubOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gitHubRetrofit = Retrofit.Builder()
        .baseUrl(GITHUB_API_URL)
        .client(gitHubOkHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val gitHubApi: GitHubApi = gitHubRetrofit.create(GitHubApi::class.java)
    
    val api: TraverseApi = retrofit.create(TraverseApi::class.java)
    
    suspend fun getLatestRelease(): NetworkResult<GitHubRelease> {
        return try {
            val release = gitHubApi.getLatestRelease()
            NetworkResult.Success(release)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun login(username: String, password: String): NetworkResult<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            response.token?.let { tokenManager.saveToken(it) }
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun register(
        username: String,
        email: String,
        password: String,
        timezone: String
    ): NetworkResult<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(username, email, password, timezone))
            response.token?.let { tokenManager.saveToken(it) }
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun logout(): NetworkResult<Unit> {
        return try {
            api.logout()
            tokenManager.deleteToken()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            // Even if server logout fails, clear local token
            tokenManager.deleteToken()
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getCurrentUser(): NetworkResult<User> {
        return try {
            val response = api.getCurrentUser()
            NetworkResult.Success(response.user)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun updateProfile(
        email: String? = null,
        timezone: String? = null,
        visibility: String? = null
    ): NetworkResult<UpdateProfileResponse> {
        return try {
            val response = api.updateProfile(UpdateProfileRequest(email, timezone, visibility))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): NetworkResult<MessageResponse> {
        return try {
            val response = api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun deleteAccount(password: String): NetworkResult<MessageResponse> {
        return try {
            val response = api.deleteAccount(DeleteAccountRequest(password))
            tokenManager.deleteToken()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getUserStats(): NetworkResult<UserStats> {
        return try {
            val response = api.getUserStats()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getSolveStats(): NetworkResult<SolveStats> {
        return try {
            val response = api.getSolveStats()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getSolves(limit: Int = 50, offset: Int = 0): NetworkResult<SolvesResponse> {
        return try {
            val response = api.getSolves(limit, offset)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getAchievementStats(): NetworkResult<AchievementStats> {
        return try {
            val response = api.getAchievementStats()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getAllAchievements(): NetworkResult<AllAchievementsResponse> {
        return try {
            val response = api.getAllAchievements()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getFreezeDates(): NetworkResult<FreezeDatesResponse> {
        return try {
            val response = api.getFreezeDates()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getGroupedRevisions(
        includeCompleted: Boolean = false,
        type: String = "normal"
    ): NetworkResult<GroupedRevisionsResponse> {
        return try {
            val response = api.getGroupedRevisions(includeCompleted, type)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getRevisionStats(type: String = "normal"): NetworkResult<RevisionStatsResponse> {
        return try {
            val response = api.getRevisionStats(type)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun completeRevision(id: Int): NetworkResult<CompleteRevisionResponse> {
        return try {
            val response = api.completeRevision(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun deleteRevision(id: Int): NetworkResult<Unit> {
        return try {
            api.deleteRevision(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun recordRevisionAttempt(
        id: Int,
        outcome: Int,
        numTries: Int,
        timeSpentMinutes: Double
    ): NetworkResult<RevisionAttemptResponse> {
        return try {
            val response = api.recordRevisionAttempt(
                id, 
                RevisionAttemptRequest(outcome, numTries, timeSpentMinutes)
            )
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    // MARK: - Friends API Methods
    
    suspend fun getFriends(): NetworkResult<FriendsListResponse> {
        return try {
            val response = api.getFriends()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getReceivedFriendRequests(): NetworkResult<FriendRequestsResponse> {
        return try {
            val response = api.getReceivedFriendRequests()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getSentFriendRequests(): NetworkResult<FriendRequestsResponse> {
        return try {
            val response = api.getSentFriendRequests()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun sendFriendRequest(username: String): NetworkResult<SendFriendRequestResponse> {
        return try {
            val response = api.sendFriendRequest(SendFriendRequestBody(username))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun acceptFriendRequest(id: Int): NetworkResult<AcceptFriendRequestResponse> {
        return try {
            val response = api.acceptFriendRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun rejectFriendRequest(id: Int): NetworkResult<FriendRequestActionResponse> {
        return try {
            val response = api.rejectFriendRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun cancelFriendRequest(id: Int): NetworkResult<FriendRequestActionResponse> {
        return try {
            val response = api.cancelFriendRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun removeFriend(username: String): NetworkResult<RemoveFriendResponse> {
        return try {
            val response = api.removeFriend(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    // MARK: - Friend Streaks API Methods
    
    suspend fun getFriendStreaks(): NetworkResult<FriendStreaksResponse> {
        return try {
            val response = api.getFriendStreaks()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getReceivedFriendStreakRequests(): NetworkResult<FriendStreakRequestsResponse> {
        return try {
            val response = api.getReceivedFriendStreakRequests()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getSentFriendStreakRequests(): NetworkResult<FriendStreakRequestsResponse> {
        return try {
            val response = api.getSentFriendStreakRequests()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun sendFriendStreakRequest(request: SendFriendStreakRequestBody): NetworkResult<SendFriendStreakRequestResponse> {
        return try {
            val response = api.sendFriendStreakRequest(request)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun acceptFriendStreakRequest(id: Int): NetworkResult<AcceptFriendStreakRequestResponse> {
        return try {
            val response = api.acceptFriendStreakRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun rejectFriendStreakRequest(id: Int): NetworkResult<MessageResponse> {
        return try {
            val response = api.rejectFriendStreakRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun cancelFriendStreakRequest(id: Int): NetworkResult<MessageResponse> {
        return try {
            val response = api.cancelFriendStreakRequest(id)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun deleteFriendStreak(username: String): NetworkResult<MessageResponse> {
        return try {
            val response = api.deleteFriendStreak(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun searchUsers(query: String, limit: Int = 20): NetworkResult<UsersSearchResponse> {
        return try {
            val response = api.searchUsers(query, limit)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getUserProfile(username: String): NetworkResult<UserProfileResponse> {
        return try {
            val response = api.getUserProfile(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getUserStatistics(username: String): NetworkResult<UserStatisticsResponse> {
        return try {
            val response = api.getUserStatistics(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getFriendSolves(username: String, limit: Int = 50, offset: Int = 0): NetworkResult<FriendSolvesResponse> {
        return try {
            val response = api.getFriendSolves(username, limit, offset)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getFriendStats(username: String): NetworkResult<UserStatisticsResponse> {
        return try {
            val response = api.getFriendStats(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getFriendAchievements(username: String): NetworkResult<FriendAchievementsResponse> {
        return try {
            val response = api.getFriendAchievements(username)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    // MARK: - Freeze Shop
    
    suspend fun getFreezeInfo(): NetworkResult<FreezeInfoResponse> {
        return try {
            val response = api.getFreezeInfo()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun purchaseFreezes(count: Int): NetworkResult<PurchaseFreezeResponse> {
        return try {
            val response = api.purchaseFreezes(PurchaseFreezeRequest(count))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun giftFreezes(username: String, count: Int): NetworkResult<GiftFreezeResponse> {
        return try {
            val response = api.giftFreezes(username, GiftFreezeRequest(count))
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    suspend fun getSubscriptionStatus(): NetworkResult<SubscriptionStatusResponse> {
        return try {
            val response = api.getSubscriptionStatus()
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(parseError(e))
        }
    }
    
    fun isAuthenticated(): Boolean = tokenManager.isAuthenticated()
    
    private fun parseError(e: Exception): String {
        return when (e) {
            is retrofit2.HttpException -> {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    errorBody?.let { 
                        json.decodeFromString<ErrorResponse>(it).error 
                    } ?: "Unknown error occurred"
                } catch (_: Exception) {
                    e.message ?: "Unknown error occurred"
                }
            }
            else -> e.message ?: "Network error occurred"
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: NetworkService? = null
        
        fun getInstance(context: Context): NetworkService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
