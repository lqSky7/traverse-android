package com.traverse.android.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Client-side storage for the user's custom streak goal.
 * Persists the selected goal locally using SharedPreferences.
 */
class StreakGoalPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStreakGoal(): Int = prefs.getInt(KEY_STREAK_GOAL, DEFAULT_GOAL)

    fun setStreakGoal(goal: Int) {
        prefs.edit().putInt(KEY_STREAK_GOAL, goal.coerceIn(MIN_GOAL, MAX_GOAL)).apply()
    }

    companion object {
        private const val PREFS_NAME = "traverse_streak_goal"
        private const val KEY_STREAK_GOAL = "streak_goal"
        const val DEFAULT_GOAL = 7
        const val MIN_GOAL = 1
        const val MAX_GOAL = 365

        @Volatile
        private var instance: StreakGoalPreferences? = null

        fun getInstance(context: Context): StreakGoalPreferences =
            instance ?: synchronized(this) {
                instance ?: StreakGoalPreferences(context.applicationContext).also { instance = it }
            }
    }
}
