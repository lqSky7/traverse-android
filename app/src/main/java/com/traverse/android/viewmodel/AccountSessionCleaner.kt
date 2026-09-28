package com.traverse.android.viewmodel

import android.content.Context
import com.traverse.android.data.CacheManager
import com.traverse.android.data.DataManager
import com.traverse.android.data.TokenManager
import com.traverse.android.ui.components.AchievementToastManager

/** Wipes all account-owned local state after the remote logout attempt completes. */
internal class AccountSessionCleaner(context: Context) {
    private val appContext = context.applicationContext

    fun clear() {
        TokenManager.getInstance(appContext).clearAllSecrets()
        CacheManager.getInstance(appContext).clearAllCache()
        DataManager.getInstance(appContext).clearAllData()
        AchievementToastManager.getInstance(appContext).resetState()
    }
}
