package com.traverse.android.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Helpers for the WorkOS social sign-in round trip.
 *
 * The flow is:
 *  1. The backend hands us a WorkOS authorization URL.
 *  2. We open it in a Custom Tab.
 *  3. WorkOS redirects to [REDIRECT_URI] (`traverse-android://auth/callback?code=...`),
 *     which the manifest routes back into `MainActivity`.
 *  4. The `code` is exchanged for a Traverse session through the backend.
 */
object SocialAuth {

    /**
     * Custom scheme registered in `AndroidManifest.xml` and as a redirect URI in
     * the WorkOS dashboard.
     */
    const val CALLBACK_SCHEME = "traverse-android"

    const val REDIRECT_URI = "$CALLBACK_SCHEME://auth/callback"

    /** Opens [url] in a Custom Tab, falling back to the default browser. */
    fun launch(context: Context, url: String) {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        } catch (e: Exception) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** Pulls the OAuth `code` out of the redirect intent, if this intent is ours. */
    fun extractCode(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (!CALLBACK_SCHEME.equals(data.scheme, ignoreCase = true)) return null
        return data.getQueryParameter("code")?.takeIf { it.isNotBlank() }
    }
}
