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

// MARK: - Friend Solves (now returns the same shape as home solves)
@Serializable
data class FriendSolvesResponse(
    val username: String,
    val solves: List<Solve>,
    val pagination: Pagination
)

// MARK: - Friend Achievements (now returns the same shape as home awards)
typealias FriendAchievementsResponse = AllAchievementsResponse

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

// MARK: - Relationship State
//
// The server's authoritative answer to "what is my relationship with this user".
// The client renders the button from `status` and never infers it — the previous
// Android implementation cross-referenced the friends list plus both request
// lists (eight parallel calls) to render one button.
@Serializable
data class RelationshipState(
    /** One of: self, none, pending_outgoing, pending_incoming, friends, blocked. */
    val status: String,
    val username: String,
    /** Whether an "Add friend" action is available. False when either side has blocked the other. */
    val canRequest: Boolean,
    val blockedByMe: Boolean,
    val friendship: RelationshipFriendship? = null,
    val streak: RelationshipStreak? = null,
    val pendingRequest: RelationshipPendingRequest? = null
) {
    val isFriends: Boolean get() = status == "friends"
    val isBlocked: Boolean get() = status == "blocked"
    val isSelf: Boolean get() = status == "self"
}

@Serializable
data class RelationshipFriendship(
    val createdAt: String,
    val favorite: Boolean
)

@Serializable
data class RelationshipStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastIncrementDate: String? = null,
    val brokenAt: String? = null,
    /** True once a day has passed without the streak advancing. */
    val atRisk: Boolean = false
)

@Serializable
data class RelationshipPendingRequest(
    val id: Int,
    /** "outgoing" if this user sent it, "incoming" if it is waiting for them. */
    val direction: String,
    val createdAt: String
)

@Serializable
data class SetFavoriteRequest(
    val favorite: Boolean
)

