package com.traverse.android.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.theme.palettePrimary
import kotlinx.coroutines.launch

/**
 * The setup walkthrough, shown before login on a fresh install.
 *
 * One step per page, swipeable, and each page is exactly what the step is: its
 * number, its wording, and its screenshot. There is deliberately no supporting
 * paragraph — the screenshots are annotated and already carry the instruction,
 * so prose underneath them only restates what the reader can see.
 *
 * Reached from `MainActivity` before `AuthNavigation` is ever composed, so it
 * cannot be confused with signing in. `Skip` and `Get Started` both call
 * [onFinish]; the caller is responsible for remembering that it was seen.
 */
@Composable
fun SetupStepsScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SETUP_STEPS.size })
    val scope = rememberCoroutineScope()
    val isLastStep = pagerState.currentPage == SETUP_STEPS.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Header — where you are, and the way out.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${pagerState.currentPage + 1} of ${SETUP_STEPS.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onFinish) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                SetupStepPage(step = SETUP_STEPS[page])
            }

            // Progress — the same five steps, at a glance.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in SETUP_STEPS.indices) {
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (selected) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) palettePrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (isLastStep) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palettePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isLastStep) "Get Started" else "Next",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Reserve the row either way so the primary button never shifts
                // as the reader moves between the first and later steps. Back is
                // dimmed on step 1 rather than removed — a Back button that
                // vanishes on the first step reads as a bug.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Text(
                            text = "Back",
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (pagerState.currentPage > 0) 0.7f else 0.3f
                            ),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepPage(step: SetupStep) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palettePrimary.copy(alpha = 0.15f))
                .border(1.dp, palettePrimary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.number.toString(),
                color = palettePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        StepScreenshot(imageRes = step.imageRes)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The screenshot, framed.
 *
 * Sized from the painter's own intrinsic width rather than stretched to the
 * page: the five images were cropped by hand and have wildly different shapes
 * — a 1400px-wide web store page next to a 140px toolbar crop — so filling the
 * width would blow the small one up into a blur. Treating source pixels as dp
 * gives every image its natural size, and the narrow container is then the only
 * thing that ever scales one down.
 */
@Composable
private fun StepScreenshot(@DrawableRes imageRes: Int) {
    val shape = RoundedCornerShape(20.dp)
    val painter = painterResource(imageRes)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = shape
            )
            .padding(10.dp)
    ) {
        val naturalWidth = painter.intrinsicSize.width.dp
        val width = if (naturalWidth.value.isFinite() && naturalWidth < maxWidth) naturalWidth else maxWidth

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .width(width)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
