/*
 * MIT License
 *
 * Copyright (c) 2026 Neural Core Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.onisong.exe.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Stable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

// ============================================================================
// SECTION 1: DATA MODELS & ENUMERATIONS
// ============================================================================

/**
 * Represents the various phases of neural thought processing.
 * Aligned with LogicAuditor phase pipeline.
 */
enum class ThoughtPhase {
    INGEST,      // Initial data intake phase
    CLASSIFY,    // Classification and categorization phase
    VALIDATE,    // Verification and validation phase
    AUDIT,       // Audit and compliance checking phase
    SYNTHESISE,  // Information synthesis phase
    GOVERN,      // Governance and policy enforcement phase
    COMMIT       // Final commit/output phase
}

/**
 * Represents the visual state of the neural core.
 * Determines animations, colors, and behaviors.
 */
enum class CoreVisualState {
    PULSING,     // Rhythmic pulse animation (rounds 1-6)
    VETOED,      // Error state with crimson shake
    SUCCESS      // Completion state with neon purple glow (round 7)
}

/**
 * Immutable data class representing the complete UI state of the Neural Core.
 *
 * @param currentRound The current processing round (1-7). Rounds 1-6 trigger pulsing,
 *                     round 7 triggers the success state.
 * @param isVetoed When true, overrides other states to show error visualization.
 * @param thoughtPhase The current cognitive phase being displayed.
 * @param isTouched Indicates if the user is currently touching the core,
 *                  which suspends gravity physics.
 */
data class CoreUIState(
    val currentRound: Int = 1,
    val isVetoed: Boolean = false,
    val thoughtPhase: ThoughtPhase = ThoughtPhase.INGEST,
    val isTouched: Boolean = false
) {
    /**
     * Derives the visual state based on current properties.
     * Priority: Vetoed > Success (Round 7) > Pulsing (Rounds 1-6)
     */
    val visualState: CoreVisualState
        get() = when {
            isVetoed -> CoreVisualState.VETOED
            currentRound == 7 -> CoreVisualState.SUCCESS
            else -> CoreVisualState.PULSING
        }

    init {
        require(currentRound in 1..7) {
            "currentRound must be between 1 and 7, got: $currentRound"
        }
    }
}

// ============================================================================
// SECTION 2: COLOR DEFINITIONS
// ============================================================================

/**
 * Centralized color palette for the Neural Core visualization.
 */
private object CoreColors {
    // Pulsing/Thinking state: Deep muted slate grey with faint purple
    val pulsingCore = Color(0xFF2F2F2F)
    val pulsingGlow = Color(0xFF3D3D4A)
    val pulsingGlowOuter = Color(0x30483D5C)
    val pulsingGlowFaintPurple = Color(0x206A5ACD)

    // Vetoed/Error state: Crimson red
    val vetoedCore = Color(0xFFDC143C)
    val vetoedGlow = Color(0xFFFF4444)
    val vetoedGlowOuter = Color(0x60DC143C)

    // Success state: Neon purple with HIGH intensity
    val successCore = Color(0xFFBF00FF)
    val successGlow = Color(0xFFDA70D6)
    val successGlowOuter = Color(0x80BF00FF)
    val successGlowIntense = Color(0xC0BF00FF)
    val successGlowUltra = Color(0xE0BF00FF)

    // Text colors
    val phaseTextPrimary = Color(0xFFE0E0E0)
}

// ============================================================================
// SECTION 3: PHYSICS ENGINE
// ============================================================================

/**
 * Physics configuration for gravity simulation.
 */
private object PhysicsConfig {
    const val GRAVITY_ACCELERATION = 0.15f
    const val MAX_FALL_VELOCITY = 3.0f
    const val DAMPING_FACTOR = 0.92f
    const val PHYSICS_FRAME_DELAY_MS = 16L
}

/**
 * Manages the physics state for gravity simulation.
 */
@Stable
class GravityPhysicsState(
    initialOffsetY: Float = 0f,
    private val maxOffsetY: Float = 200f
) {
    var offsetY by mutableFloatStateOf(initialOffsetY)
        private set

    var velocityY by mutableFloatStateOf(0f)
        private set

    /**
     * Updates physics simulation for one frame.
     * Applies gravity acceleration and updates position.
     */
    fun updatePhysics() {
        velocityY = (velocityY + PhysicsConfig.GRAVITY_ACCELERATION)
            .coerceAtMost(PhysicsConfig.MAX_FALL_VELOCITY)
        offsetY = (offsetY + velocityY).coerceIn(-maxOffsetY, maxOffsetY)
    }

    /**
     * Called when the user touches the core.
     */
    fun onTouch() {
        velocityY = 0f
    }

    /**
     * Gradually returns the core to center when touched.
     */
    fun applyDamping() {
        offsetY *= PhysicsConfig.DAMPING_FACTOR
        if (abs(offsetY) < 0.5f) {
            offsetY = 0f
        }
    }
}

/**
 * Composable effect that runs the gravity physics loop.
 */
@Composable
private fun GravityEffect(
    physicsState: GravityPhysicsState,
    isActive: Boolean
) {
    LaunchedEffect(isActive) {
        while (true) {
            if (isActive) {
                physicsState.updatePhysics()
            } else {
                physicsState.applyDamping()
            }
            delay(PhysicsConfig.PHYSICS_FRAME_DELAY_MS)
        }
    }
}

// ============================================================================
// SECTION 4: ANIMATION CONTROLLERS
// ============================================================================

private object AnimationConfig {
    const val PULSE_DURATION_MS = 1200
    const val GLOW_PULSE_DURATION_MS = 1500
    const val SHAKE_DURATION_MS = 100
    const val SUCCESS_GLOW_DURATION_MS = 1600
    const val COLOR_TRANSITION_MS = 300
}

@Stable
data class CoreAnimations(
    val pulseScale: Float,
    val glowAlpha: Float,
    val glowRadius: Float,
    val shakeOffset: Float,
    val successGlowIntensity: Float
)

@Composable
private fun rememberCoreAnimations(visualState: CoreVisualState): CoreAnimations {
    val infiniteTransition = rememberInfiniteTransition(label = "coreAnimations")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.PULSE_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.GLOW_PULSE_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.GLOW_PULSE_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.SHAKE_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    val successGlowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.SUCCESS_GLOW_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successGlowIntensity"
    )

    return CoreAnimations(
        pulseScale = if (visualState == CoreVisualState.PULSING) pulseScale else 1f,
        glowAlpha = glowAlpha,
        glowRadius = glowRadius,
        shakeOffset = if (visualState == CoreVisualState.VETOED) shakeOffset else 0f,
        successGlowIntensity = if (visualState == CoreVisualState.SUCCESS) successGlowIntensity else 0f
    )
}

// ============================================================================
// SECTION 5: DRAWING UTILITIES
// ============================================================================

private fun DrawScope.drawGlowingCore(
    center: Offset,
    baseRadius: Float,
    visualState: CoreVisualState,
    animations: CoreAnimations
) {
    val (coreColor, glowColor, outerGlowColor) = when (visualState) {
        CoreVisualState.PULSING -> Triple(
            CoreColors.pulsingCore,
            CoreColors.pulsingGlow,
            CoreColors.pulsingGlowOuter
        )
        CoreVisualState.VETOED -> Triple(
            CoreColors.vetoedCore,
            CoreColors.vetoedGlow,
            CoreColors.vetoedGlowOuter
        )
        CoreVisualState.SUCCESS -> Triple(
            CoreColors.successCore,
            CoreColors.successGlow,
            CoreColors.successGlowOuter
        )
    }

    val animatedRadius = baseRadius * animations.pulseScale
    val animatedGlowRadius = animatedRadius * animations.glowRadius * 2.5f

    // Layer 0: Ultra outer glow for success
    if (visualState == CoreVisualState.SUCCESS) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CoreColors.successGlowUltra.copy(alpha = animations.successGlowIntensity * 0.5f),
                    CoreColors.successGlowIntense.copy(alpha = animations.successGlowIntensity * 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = animatedGlowRadius * 1.8f
            ),
            radius = animatedGlowRadius * 1.8f,
            center = center
        )
    }

    // Layer 1: Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                outerGlowColor.copy(alpha = animations.glowAlpha * 0.5f),
                Color.Transparent
            ),
            center = center,
            radius = animatedGlowRadius
        ),
        radius = animatedGlowRadius,
        center = center
    )

    // Layer 1.5: Faint purple accent for pulsing
    if (visualState == CoreVisualState.PULSING) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CoreColors.pulsingGlowFaintPurple.copy(alpha = animations.glowAlpha * 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = animatedGlowRadius * 1.1f
            ),
            radius = animatedGlowRadius * 1.1f,
            center = center
        )
    }

    // Layer 2: Success intense ring
    if (visualState == CoreVisualState.SUCCESS) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CoreColors.successGlowIntense.copy(alpha = animations.successGlowIntensity * 0.85f),
                    CoreColors.successGlowOuter.copy(alpha = animations.successGlowIntensity * 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = animatedGlowRadius * 1.4f
            ),
            radius = animatedGlowRadius * 1.4f,
            center = center
        )
    }

    // Layer 3: Mid glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = animations.glowAlpha),
                outerGlowColor.copy(alpha = animations.glowAlpha * 0.3f),
                Color.Transparent
            ),
            center = center,
            radius = animatedRadius * 1.8f
        ),
        radius = animatedRadius * 1.8f,
        center = center
    )

    // Layer 4: Inner glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor,
                coreColor.copy(alpha = 0.7f),
                Color.Transparent
            ),
            center = center,
            radius = animatedRadius * 1.3f
        ),
        radius = animatedRadius * 1.3f,
        center = center
    )

    // Layer 5: Solid core
    val coreCenterColor = when (visualState) {
        CoreVisualState.SUCCESS -> Color.White.copy(alpha = 0.95f)
        CoreVisualState.VETOED -> Color.White.copy(alpha = 0.85f)
        CoreVisualState.PULSING -> Color(0xFF4A4A4A)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreCenterColor, glowColor, coreColor),
            center = center,
            radius = animatedRadius
        ),
        radius = animatedRadius,
        center = center
    )
}

// ============================================================================
// SECTION 6: UI COMPONENTS
// ============================================================================

@Composable
private fun PhaseIndicator(
    phase: ThoughtPhase,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = CoreColors.phaseTextPrimary,
        animationSpec = tween(durationMillis = AnimationConfig.COLOR_TRANSITION_MS),
        label = "phaseTextColor"
    )

    Text(
        text = phase.name,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}

@Composable
private fun NeuralCoreCanvas(
    visualState: CoreVisualState,
    animations: CoreAnimations,
    physicsOffset: Float,
    shakeOffset: Float,
    coreRadius: Dp,
    onTouch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coreRadiusPx = with(density) { coreRadius.toPx() }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTouch()
                        tryAwaitRelease()
                    }
                )
            }
    ) {
        val centerX = size.width / 2 + shakeOffset
        val centerY = size.height / 2 + physicsOffset
        val center = Offset(centerX, centerY)

        drawGlowingCore(
            center = center,
            baseRadius = coreRadiusPx,
            visualState = visualState,
            animations = animations
        )
    }
}

// ============================================================================
// SECTION 7: MAIN COMPOSABLE
// ============================================================================

/**
 * Main Neural Core View composable optimized for Android overlay context.
 *
 * Designed for Window.LayoutParams.TYPE_APPLICATION_OVERLAY with internal
 * padding to avoid system status bars.
 *
 * @param state Current UI state controlling visualization
 * @param modifier Optional modifier for customization
 * @param coreRadius Size of the central core circle
 * @param statusBarPadding Padding to avoid status bar
 * @param navigationBarPadding Padding to avoid navigation bar
 * @param onStateChange Callback when internal state changes occur
 */
@Composable
fun NeuralCoreView(
    state: CoreUIState,
    modifier: Modifier = Modifier,
    coreRadius: Dp = 40.dp,
    statusBarPadding: Dp = 24.dp,
    navigationBarPadding: Dp = 48.dp,
    onStateChange: ((CoreUIState) -> Unit)? = null
) {
    // Physics state: gravity pulls core down unless touched
    val physicsState = remember { GravityPhysicsState() }

    // Run gravity loop: active when NOT touched
    GravityEffect(
        physicsState = physicsState,
        isActive = !state.isTouched
    )

    // Handle touch: stop gravity, begin damping back to center
    LaunchedEffect(state.isTouched) {
        if (state.isTouched) {
            physicsState.onTouch()
        }
    }

    // Animation state
    val animations = rememberCoreAnimations(state.visualState)

    // Main layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(
                top = statusBarPadding,
                bottom = navigationBarPadding,
                start = 16.dp,
                end = 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =    Arrangement.Center
        ) {
            NeuralCoreCanvas(
                visualState = state.visualState,
                animations = animations,
                physicsOffset = physicsState.offsetY,
                shakeOffset = animations.shakeOffset,
                coreRadius = coreRadius,
                onTouch = { onStateChange?.invoke(state.copy(isTouched = true)) },
                modifier = Modifier.size(coreRadius * 6)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PhaseIndicator(
                phase = state.thoughtPhase,
                modifier = Modifier.padding(top = physicsState.offsetY.dp)
            )
        }
    }
}
