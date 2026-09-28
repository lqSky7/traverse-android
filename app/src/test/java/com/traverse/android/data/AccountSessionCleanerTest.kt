package com.traverse.android.data

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.traverse.android.ui.components.AchievementToastManager
import com.traverse.android.viewmodel.AccountSessionCleaner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountSessionCleanerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        CacheManager.getInstance(context).clearAllCache()
        DataManager.getInstance(context).clearAllData()
        AchievementToastManager.getInstance(context).resetState()
    }

    @Test
    fun logoutClearsPreferencesImagesAndPersistedAccountFiles() {
        val cache = CacheManager.getInstance(context)
        val avatar = File(context.filesDir, "profile_image_test.jpg").apply { writeText("avatar") }
        cache.cacheExamMode(true)
        cache.cacheProfileImageFile(avatar.absolutePath)
        cache.cachePushToken("push-token")

        listOf("friends.json", "awardSections.json", "lastFetchTimestamp.json").forEach {
            File(context.filesDir, it).writeText("account data")
        }

        AccountSessionCleaner(context).clear()

        assertFalse(cache.getExamMode())
        assertNull(cache.getPushToken())
        assertFalse(avatar.exists())
        assertTrue(listOf("friends.json", "awardSections.json", "lastFetchTimestamp.json")
            .none { File(context.filesDir, it).exists() })
        assertTrue(DataManager.getInstance(context).friends.value.isEmpty())
        assertTrue(DataManager.getInstance(context).awardSections.value.isEmpty())
        assertNull(TokenManager.getInstance(context).getToken())
    }
}
