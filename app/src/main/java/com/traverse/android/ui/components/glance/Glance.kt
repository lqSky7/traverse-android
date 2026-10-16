package com.traverse.android.ui.components.glance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.components.glass.GlassBackdrop
import com.traverse.android.ui.components.glass.Glur
import com.traverse.android.ui.components.glass.GlurSegment
import com.traverse.android.ui.components.glass.ProgressiveHeaderScrim
import com.traverse.android.ui.components.glass.liquidGlass
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette

/**
 * The Android port of the iOS quick-glance header: a pinned title row above a
 * row of wide, short glass cards that surface one number each, with a
 * progressive scrim behind them so the feed dissolves as it scrolls under.
 *
 * The action chips (freezes, notifications, profile…) are deliberately NOT
 * part of the header — screens put an [ActionChipRow] as the first item of
 * their scroll content, so the chips scroll away and the feed takes their
 * place, exactly like iOS.
 */

// MARK: - Models

/** One quick-glance card. */
data class GlanceCard(
    val id: String,
    val icon: ImageVector,
    val value: String,
    val label: String,
    val tint: Color,
    val badge: Int? = null,
    /** 0f..1f — draws a mini progress ring. */
    val progress: Float? = null,
    /** Optional bar trend, oldest → newest. */
    val sparkline: List<Float>? = null,
    val onClick: (() -> Unit)? = null
)

/** One action chip. */
data class ActionChip(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val tint: Color,
    val badge: Int? = null,
    /** When set, the chip shows this image in a circle instead of the icon. */
    val avatarUrl: String? = null,
    val onClick: () -> Unit
)

// MARK: - Metrics

/** Height of the card row inside the header. */
private val CardRowHeight = 64.dp

/** Width of one card. */
private val CardWidth = 188.dp

/** Height of one card. */
private val CardHeight = 62.dp

/** Uniform size every action chip is normalized to. */
private val ChipWidth = 118.dp
private val ChipHeight = 44.dp

/**
 * Height of the header content (title row + cards), excluding the status bar.
 * Screens offset their scroll content by this so it starts below the header.
 */
val GlanceHeaderHeight: Dp = 40.dp + 8.dp + CardRowHeight + 10.dp

// MARK: - Header

/**
 * The pinned header. Draw it in a `Box` above the scrolling content, and give
 * the content `contentPadding.top = GlanceHeaderHeight + statusBar`.
 */
@Composable
fun GlanceScreenHeader(
    title: String,
    cards: List<GlanceCard>,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    onBack: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (backdrop != null) {
            // The Glur: the live feed scrolling underneath is blurred and
            // ramped out toward the header's bottom edge, so it dissolves
            // under the header instead of stopping at a hard black band.
            Glur(
                backdrop = backdrop,
                modifier = Modifier.matchParentSize(),
                radius = 16.dp,
                segment = GlurSegment.Single
            )
        } else {
            // No capture to blur: fall back to the flat scrim, which at least
            // keeps the header legible over the feed. It draws a little past
            // the header's bottom edge (drawing isn't clipped), so the fade
            // never ends on a hard line.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val overhang = 24.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black,
                                    0.45f to Color.Black.copy(alpha = 0.9f),
                                    0.8f to Color.Black.copy(alpha = 0.45f),
                                    1f to Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height + overhang
                            )
                        )
                    }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = RingiftFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            if (cards.isNotEmpty()) {
                GlanceCardRow(cards = cards)
            }
        }
    }
}

/** The scrim alone — put it behind a custom header if you're not using [GlanceScreenHeader]. */
@Composable
fun GlanceScrim(modifier: Modifier = Modifier) {
    ProgressiveHeaderScrim(modifier = modifier.fillMaxWidth())
}

// MARK: - Card row

@Composable
fun GlanceCardRow(cards: List<GlanceCard>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(CardRowHeight),
        // Inset from the screen edges, like the rest of the content.
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(cards, key = { it.id }) { card ->
            GlanceCardView(card)
        }
    }
}

@Composable
private fun GlanceCardView(card: GlanceCard) {
    Row(
        modifier = Modifier
            .size(width = CardWidth, height = CardHeight)
            .liquidGlass(shape = RoundedCornerShape(18.dp))
            .then(if (card.onClick != null) Modifier.clickable(onClick = card.onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon chip — identical on every card so nothing renders inconsistently.
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(card.tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = card.icon,
                contentDescription = null,
                tint = card.tint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = card.value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RingiftFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
                card.badge?.takeIf { it > 0 }?.let { badge ->
                    Text(
                        text = if (badge > 9) "9+" else "$badge",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.Black,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(card.tint)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = card.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }

        // Metric-matched mini visualization.
        when {
            card.progress != null -> MiniRing(progress = card.progress, tint = card.tint)
            !card.sparkline.isNullOrEmpty() -> MiniSparkline(values = card.sparkline, tint = card.tint)
        }
    }
}

// MARK: - Action chips

@Composable
fun ActionChipRow(chips: List<ActionChip>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(ChipHeight),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(chips, key = { it.id }) { chip ->
            ActionChipView(chip)
        }
    }
}

@Composable
private fun ActionChipView(chip: ActionChip) {
    Row(
        modifier = Modifier
            // Normalized: every chip is identical, sized for the biggest label.
            .size(width = ChipWidth, height = ChipHeight)
            .liquidGlass(shape = RoundedCornerShape(18.dp))
            .clickable(onClick = chip.onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (chip.avatarUrl != null) {
            AvatarImage(url = chip.avatarUrl, size = 22.dp)
        } else {
            Icon(
                imageVector = chip.icon,
                contentDescription = null,
                tint = chip.tint,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = chip.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1
        )

        chip.badge?.takeIf { it > 0 }?.let { badge ->
            Text(
                text = if (badge > 9) "9+" else "$badge",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = Color.Black,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(chip.tint)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

/** Profile photo in a circle. Falls back to a neutral disc while loading. */
@Composable
private fun AvatarImage(url: String, size: Dp) {
    coil.compose.AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    )
}

// MARK: - Refresh loader

/**
 * The app's own pull-to-refresh indicator: a palette-coloured ring that spins,
 * in a small glass capsule.
 *
 * Material's default indicator is a plain grey spinner; this matches the
 * iOS build's loader and the app's palette instead. Use it as the
 * `indicator` slot of a `PullToRefreshBox`.
 */
@Composable
fun TraverseRefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()

    // The sweep runs continuously; the whole capsule only exists while
    // refreshing, so there is nothing to keep in sync.
    val transition = rememberInfiniteTransition(label = "refreshSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshAngle"
    )

    val arcColors = listOf(
        palette.colorAt(0),
        palette.colorAt(1),
        palette.colorAt(2),
        palette.primary
    )

    AnimatedVisibility(
        visible = isRefreshing,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .liquidGlass(shape = RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(16.dp)
                    .rotate(angle)
            ) {
                val stroke = 2.5.dp.toPx()
                val inset = stroke / 2f
                drawArc(
                    brush = Brush.sweepGradient(arcColors),
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Text(
                text = "Refreshing",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// MARK: - Mini visualizations

/** Mini progress ring for anything expressed as a percentage. */
@Composable
private fun MiniRing(progress: Float, tint: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .drawBehind {
                val stroke = 3.5.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
            }
    )
}

/** Tiny bar trend, normalized against the tallest value. */
@Composable
private fun MiniSparkline(values: List<Float>, tint: Color) {
    val peak = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .drawBehind {
                val barWidth = 4.dp.toPx()
                val gap = 2.5.dp.toPx()
                values.forEachIndexed { index, value ->
                    val fraction = (value / peak).coerceAtLeast(0.12f)
                    val barHeight = size.height * fraction
                    drawRoundRect(
                        color = tint.copy(alpha = if (index == values.lastIndex) 1f else 0.45f),
                        topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                    )
                }
            }
    )
}
