package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Facebook Shimmer effect modifier
 * Sweeps an ultra-smooth high-contrast highlight band across the element from left to right.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(6.dp),
    durationMillis: Int = 1100
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "facebook_shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -800f,
        targetValue = 2200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "facebook_shimmer_translation"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseColor = if (isDark) Color(0xFF26292E) else Color(0xFFD6DBE1)
    val highlightColor = if (isDark) Color(0xFF4A505B) else Color(0xFFFFFFFF)

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim * 0.35f - 400f),
        end = Offset(translateAnim + 400f, translateAnim * 0.35f + 400f)
    )

    this
        .clip(shape)
        .background(brush)
}

/**
 * Shimmer element block with customizable size and rounded corners
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    val sizeModifier = if (width != null) {
        modifier
            .width(width)
            .height(height)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }
    Box(
        modifier = sizeModifier.shimmerEffect(shape = shape)
    )
}

/**
 * Facebook-style Skeleton container with smooth Crossfade transition
 */
@Composable
fun FacebookShimmerCrossfade(
    isLoading: Boolean,
    skeletonContent: @Composable () -> Unit,
    actualContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        modifier = modifier.fillMaxSize(),
        label = "facebook_shimmer_crossfade"
    ) { loading ->
        if (loading) {
            skeletonContent()
        } else {
            actualContent()
        }
    }
}

/**
 * Facebook Home / Dashboard Skeleton
 */
@Composable
fun FacebookDashboardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Stats Skeleton
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShimmerBox(width = 150.dp, height = 22.dp, shape = RoundedCornerShape(8.dp))
                    Box(modifier = Modifier.size(38.dp).shimmerEffect(CircleShape))
                }
                ShimmerBox(width = 240.dp, height = 14.dp, shape = RoundedCornerShape(6.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShimmerBox(modifier = Modifier.weight(1f), height = 54.dp, shape = RoundedCornerShape(12.dp))
                    ShimmerBox(modifier = Modifier.weight(1f), height = 54.dp, shape = RoundedCornerShape(12.dp))
                }
            }
        }

        // Quick Actions Grid Skeleton (4 cards)
        ShimmerBox(width = 160.dp, height = 18.dp, shape = RoundedCornerShape(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 90.dp, shape = RoundedCornerShape(14.dp))
            ShimmerBox(modifier = Modifier.weight(1f), height = 90.dp, shape = RoundedCornerShape(14.dp))
        }

        // Recent Surveys Section Skeleton
        ShimmerBox(width = 180.dp, height = 18.dp, shape = RoundedCornerShape(6.dp))
        repeat(2) {
            FacebookSurveyCardSkeleton()
        }
    }
}

/**
 * Facebook Survey List Skeleton (Multiple Feed-Style Post Cards)
 */
@Composable
fun FacebookSurveyListSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search bar skeleton (Underlined style)
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                ShimmerBox(width = 24.dp, height = 24.dp, shape = CircleShape)
                ShimmerBox(height = 18.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp))
                ShimmerBox(width = 20.dp, height = 16.dp, shape = RoundedCornerShape(6.dp))
            }
            ShimmerBox(height = 1.5.dp, shape = RoundedCornerShape(1.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        }

        // Filter chips skeleton row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(width = 80.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            ShimmerBox(width = 110.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            ShimmerBox(width = 95.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Survey Cards Skeletons
        repeat(3) {
            FacebookSurveyCardSkeleton()
        }
    }
}

/**
 * Facebook Post / Survey Card Item Skeleton
 */
@Composable
fun FacebookSurveyCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Action menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle Avatar skeleton (Facebook style)
                Box(modifier = Modifier.size(44.dp).shimmerEffect(CircleShape))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(width = 180.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                    ShimmerBox(width = 110.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                }

                // Badge status pill skeleton
                ShimmerBox(width = 75.dp, height = 24.dp, shape = RoundedCornerShape(12.dp))
            }

            // Body: 2 text lines
            ShimmerBox(height = 14.dp, shape = RoundedCornerShape(4.dp))
            ShimmerBox(width = 230.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))

            // Footer: Chips & action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                ShimmerBox(width = 130.dp, height = 24.dp, shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(32.dp).shimmerEffect(CircleShape))
                    Box(modifier = Modifier.size(32.dp).shimmerEffect(CircleShape))
                }
            }
        }
    }
}

/**
 * Facebook Form Screen Skeleton (Questionnaire questions list)
 */
@Composable
fun FacebookFormSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner card skeleton
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerBox(width = 220.dp, height = 20.dp, shape = RoundedCornerShape(6.dp))
                ShimmerBox(width = 160.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 6.dp, shape = RoundedCornerShape(3.dp))
            }
        }

        // Questions cards skeletons
        repeat(3) { index ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(24.dp).shimmerEffect(CircleShape))
                        ShimmerBox(width = (160 + (index * 30)).dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
                    }
                    ShimmerBox(height = 48.dp, shape = RoundedCornerShape(8.dp))
                }
            }
        }
    }
}

/**
 * Facebook Sync Screen Skeleton
 */
@Composable
fun FacebookSyncSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Center hero card skeleton
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(64.dp).shimmerEffect(CircleShape))
                ShimmerBox(width = 180.dp, height = 20.dp, shape = RoundedCornerShape(6.dp))
                ShimmerBox(width = 240.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(height = 46.dp, shape = RoundedCornerShape(23.dp))
            }
        }

        // Audit logs skeleton
        ShimmerBox(width = 150.dp, height = 18.dp, shape = RoundedCornerShape(6.dp))
        repeat(3) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(36.dp).shimmerEffect(CircleShape))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBox(width = 160.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                        ShimmerBox(width = 100.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
                    }
                }
            }
        }
    }
}
