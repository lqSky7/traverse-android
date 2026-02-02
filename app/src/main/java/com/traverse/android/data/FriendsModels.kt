package com.traverse.android.data

import kotlinx.serialization.Serializable

// MARK: - User Basic (for search results and friend request users)
@Serializable
data class UserBasic(
    val id: Int,
    val username: String,
    val currentStreak: Int,
    val totalXp: Int
)

// MARK: - User Profile
@Serializable
data class UserProfile(
    val id: Int,
    val username: String,
    val timezone: String,
    val visibility: String,
    val currentStreak: Int,
    val totalXp: Int,
    val createdAt: String? = null
)

@Serializable
data class UserProfileResponse(
    val user: UserProfile
)

// MARK: - User Statistics (from public endpoint)
@Serializable
data class UserStatisticsResponse(
    val username: String,
    val stats: UserStatisticsData
)

@Serializable
data class UserStatisticsData(
    val totalSolves: Int,
    val totalSubmissions: Int,
    val totalStreakDays: Int,
    val problemsByDifficulty: ProblemsByDifficulty = ProblemsByDifficulty()
)

// MARK: - User Search
@Serializable
data class UsersSearchResponse(
    val users: List<UserBasic>
)

// MARK: - Friend Request Models
@Serializable
data class FriendRequest(
    val id: Int,
    val status: String,
    val createdAt: String,
    val requester: UserBasic? = null,
    val addressee: UserBasic? = null
)

@Serializable
data class FriendRequestsResponse(
    val requests: List<FriendRequest>
)

@Serializable
data class SendFriendRequestBody(
    val username: String
)

@Serializable
data class SendFriendRequestResponse(
    val message: String,
    val request: FriendRequest
)

@Serializable
data class AcceptFriendRequestResponse(
    val message: String,
    val friendship: Friendship
)

@Serializable
data class Friendship(
    val createdAt: String,
    val user1: UserBasic,
    val user2: UserBasic
)

@Serializable
data class FriendRequestActionResponse(
    val message: String
)

// MARK: - Friends Models
@Serializable
data class Friend(
    val friendshipId: String,
    val friendedAt: String? = null,
    val id: Int,
    val username: String,
    val currentStreak: Int,
    val totalXp: Int,
    val visibility: String
)

@Serializable
data class FriendsListResponse(
    val friends: List<Friend>
)

@Serializable
data class RemoveFriendResponse(
    val message: String
)

// MARK: - Friend Solves
@Serializable
data class FriendSolvesResponse(
    val username: String,
    val solves: List<Solve>,
    val pagination: Pagination
)

// MARK: - Friend Achievements
@Serializable
data class FriendAchievementsResponse(
    val username: String,
    val achievements: List<FriendAchievement>
)

@Serializable
data class FriendAchievement(
    val id: Int,
    val key: String,
    val name: String,
    val description: String,
    val category: String,
    val unlockedAt: String
)

// MARK: - Profile Management
@Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    val timezone: String? = null,
    val visibility: String? = null
)

@Serializable
data class UpdateProfileResponse(
    val message: String,
    val user: UserProfile
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
data class MessageResponse(
    val message: String
)

// MARK: - Freeze Shop Models
@Serializable
data class FreezeCosts(
    val purchase: Int,
    val gift: Int
)

@Serializable
data class FreezeInfoResponse(
    val availableFreezes: Int,
    val usedFreezes: Int,
    val totalFreezes: Int,
    val costs: FreezeCosts
)

@Serializable
data class PurchaseFreezeRequest(
    val count: Int = 1
)

@Serializable
data class PurchaseFreezeResponse(
    val message: String,
    val freezesPurchased: Int,
    val xpSpent: Int,
    val availableFreezes: Int,
    val remainingXp: Int
)

@Serializable
data class GiftFreezeRequest(
    val count: Int = 1
)

@Serializable
data class GiftFreezeResponse(
    val message: String,
    val freezesGifted: Int,
    val xpSpent: Int,
    val recipientUsername: String,
    val remainingXp: Int
)

// MARK: - Friend Streak Models
@Serializable
data class FriendStreakRequest(
    val id: Int,
    val status: String,
    val createdAt: String,
    val requester: FriendStreakUser? = null,
    val requested: FriendStreakUser? = null
)

@Serializable
data class FriendStreakUser(
    val id: Int,
    val username: String,
    val currentStreak: Int
)

@Serializable
data class FriendStreak(
    val friend: FriendStreakUser,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastIncrementDate: String? = null,
    val createdAt: String
)

@Serializable
data class FriendStreakRequestsResponse(
    val requests: List<FriendStreakRequest>
)

@Serializable
data class FriendStreaksResponse(
    val streaks: List<FriendStreak>
)

@Serializable
data class SendFriendStreakRequestBody(
    val username: String
)

@Serializable
data class SendFriendStreakRequestResponse(
    val message: String,
    val request: FriendStreakRequest
)

@Serializable
data class AcceptFriendStreakRequestResponse(
    val message: String,
    val streak: FriendStreak
)

// MARK: - Subscription Status

@Serializable
data class SubscriptionStatusResponse(
    val isSubscriptionActive: Boolean
)
