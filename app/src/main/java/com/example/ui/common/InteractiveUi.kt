package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Tactile spring bounce + haptic feedback modifier that preserves scroll gestures
 * in LazyColumn/LazyRow by observing MutableInteractionSource.collectIsPressedAsState().
 */
fun Modifier.interactiveBounce(
  enabled: Boolean = true,
  pressedScale: Float = 0.965f,
  enableHaptics: Boolean = true,
  interactionSource: MutableInteractionSource? = null,
  onClick: (() -> Unit)? = null
): Modifier = composed {
  val actualSource = interactionSource ?: remember { MutableInteractionSource() }
  val isPressed by actualSource.collectIsPressedAsState()
  val haptics = LocalHapticFeedback.current

  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "interactive_bounce_scale"
  )

  val alpha by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.92f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "interactive_bounce_alpha"
  )

  val baseModifier = this.graphicsLayer {
    scaleX = scale
    scaleY = scale
    this.alpha = alpha
  }

  if (onClick != null) {
    baseModifier.clickable(
      interactionSource = actualSource,
      indication = androidx.compose.material3.ripple(),
      enabled = enabled,
      onClick = {
        if (enableHaptics) {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onClick()
      }
    )
  } else {
    baseModifier
  }
}

/**
 * Provides tactile press-shrink interaction state without overriding Card/Surface's native onClick.
 */
@Composable
fun rememberBounceInteraction(pressedScale: Float = 0.965f): Pair<MutableInteractionSource, Float> {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "bounce_interaction_scale"
  )
  return interactionSource to scale
}

/**
 * Smoothly animated integer counter for KPI numbers and live tallies.
 */
@Composable
fun AnimatedCountText(
  targetCount: Int,
  color: Color,
  fontSize: TextUnit,
  fontWeight: FontWeight = FontWeight.ExtraBold,
  fontFamily: FontFamily? = null,
  maxLines: Int = 1,
  softWrap: Boolean = false,
  modifier: Modifier = Modifier
) {
  val animatedValue by animateIntAsState(
    targetValue = targetCount,
    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    label = "animated_kpi_counter"
  )

  Text(
    text = animatedValue.toString(),
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    maxLines = maxLines,
    softWrap = softWrap,
    style = LocalTextStyle.current.merge(TextStyle(fontFeatureSettings = "tnum")),
    modifier = modifier
  )
}

/**
 * Live pulsing status dot for real-time indicators (live clock, active ops, unread alert).
 */
@Composable
fun PulsingLiveDot(
  color: Color,
  size: Dp = 8.dp,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "live_dot_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.65f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "dot_halo_scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.65f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "dot_halo_alpha"
  )

  Box(
    modifier = modifier.size(size * 2),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .scale(pulseScale)
        .alpha(pulseAlpha)
        .background(color, CircleShape)
    )
    Box(
      modifier = Modifier
        .size(size)
        .background(color, CircleShape)
    )
  }
}

/**
 * Fast, snappy staggered entrance animation (<260ms) for hero & dashboard cards.
 */
@Composable
fun StaggeredEntrance(
  delayMillis: Int = 0,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var visible by remember { mutableStateOf(delayMillis == 0) }
  LaunchedEffect(Unit) {
    if (delayMillis > 0) {
      delay(delayMillis.toLong())
    }
    visible = true
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(durationMillis = 240)) +
      slideInVertically(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioLowBouncy,
          stiffness = Spring.StiffnessMediumLow
        ),
        initialOffsetY = { it / 10 }
      ),
    modifier = modifier
  ) {
    content()
  }
}

/**
 * Animated subtle diagonal light sheen brush for Hero KPI and executive cards.
 */
@Composable
fun rememberShimmerBrush(
  highlightColor: Color = Color.White.copy(alpha = 0.14f)
): Brush {
  val transition = rememberInfiniteTransition(label = "hero_shimmer")
  val translateAnim by transition.animateFloat(
    initialValue = -300f,
    targetValue = 1200f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_offset"
  )

  return Brush.linearGradient(
    colors = listOf(
      Color.Transparent,
      highlightColor,
      Color.Transparent
    ),
    start = Offset(translateAnim, 0f),
    end = Offset(translateAnim + 280f, 220f)
  )
}
