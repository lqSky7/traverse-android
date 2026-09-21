package com.traverse.android.ui.onboarding

import androidx.annotation.DrawableRes
import com.traverse.android.R

/**
 * The setup walkthrough shown before login on a fresh install.
 *
 * Both the titles and the screenshots are the source material verbatim — the
 * wording is intentionally left exactly as it was handed over, spelling
 * included, because the point of the screen is to match the screenshots it
 * shows rather than to read well on its own. The images live in
 * `res/drawable-nodpi/` so they are never resampled by the density splitter;
 * `SetupStepsScreen` sizes them from their own intrinsic width.
 *
 * `LeetFeedback/website/src/components/SetupOnboardingSteps.tsx` renders the
 * identical five steps for the `/guide` page. If a step changes, change both.
 */
data class SetupStep(
    val number: Int,
    val title: String,
    @DrawableRes val imageRes: Int
)

val SETUP_STEPS: List<SetupStep> = listOf(
    SetupStep(
        number = 1,
        title = "Install chrome extension",
        imageRes = R.drawable.onboarding_step_1
    ),
    SetupStep(
        number = 2,
        title = "Sign in /Register to website",
        imageRes = R.drawable.onboarding_step_2
    ),
    SetupStep(
        number = 3,
        title = "open extention UI",
        imageRes = R.drawable.onboarding_step_3
    ),
    SetupStep(
        number = 4,
        title = "make sure same User is signed in",
        imageRes = R.drawable.onboarding_step_4
    ),
    SetupStep(
        number = 5,
        title = "You are done! go solve problems on your favourite platform as you normally do!",
        imageRes = R.drawable.onboarding_step_5
    )
)
