package com.traverse.android.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Open the GitHub authorization page.
 *
 * A Custom Tab rather than a bare `ACTION_VIEW`: it is the same browser engine
 * the reader is already signed into GitHub with, so the usual case is one tap
 * on "Authorize" with no login form — and it cannot be hijacked by another app
 * that has registered for the URL. Devices with no Custom Tabs provider (bare
 * AOSP, some OEM builds) fall back to a normal browser intent, and a device
 * with no browser at all is a no-op rather than a crash.
 *
 * `FLAG_ACTIVITY_NEW_TASK` is only on the fallback: `launchUrl` is given the
 * activity context, while the fallback may be reached from a non-activity one.
 */
fun openGitHubAuthPage(context: Context, url: String) {
    val uri = Uri.parse(url)

    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
        return
    } catch (_: ActivityNotFoundException) {
        // No Custom Tabs provider — try the plain browser below.
    }

    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        // No browser installed. Nothing sensible left to do.
    }
}
