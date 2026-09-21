package com.traverse.android.data

import android.content.Context
import android.content.SharedPreferences

/**
 * One boolean: has this install already been shown the setup walkthrough?
 *
 * Deliberately its own preferences file rather than an entry in [CacheManager].
 * Cache entries there carry a TTL and `clearAllCache()` wipes them on logout —
 * the walkthrough has to survive both. Signing out is not a reason to re-teach
 * somebody how to install the extension, and a TTL would make the walkthrough
 * reappear on its own weeks later.
 *
 * It is also deliberately *not* keyed on authentication: the screen is shown
 * before login, so it is a statement about this install, not about this account.
 */
class OnboardingState private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** False until [markSetupStepsSeen] has been called on this install. */
    fun hasSeenSetupSteps(): Boolean = prefs.getBoolean(KEY_SEEN_SETUP_STEPS, false)

    fun markSetupStepsSeen() {
        prefs.edit().putBoolean(KEY_SEEN_SETUP_STEPS, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "traverse_onboarding"
        private const val KEY_SEEN_SETUP_STEPS = "seen_setup_steps"

        @Volatile
        private var INSTANCE: OnboardingState? = null

        fun getInstance(context: Context): OnboardingState {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingState(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
