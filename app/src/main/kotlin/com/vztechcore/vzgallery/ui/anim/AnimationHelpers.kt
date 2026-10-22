package com.vztechcore.vzgallery.ui.anim

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.TransformOrigin
import com.vztechcore.vzgallery.data.AnimationMode

enum class ScreenSlideDirection { RIGHT, LEFT, NONE }

/**
 * Decides direction based on screen "depth".
 * If the new screen is deeper → enters from the right.
 * If it's shallower → enters from the left.
 */
fun depthDirection(fromDepth: Int, toDepth: Int): ScreenSlideDirection = when {
    toDepth > fromDepth -> ScreenSlideDirection.RIGHT
    toDepth < fromDepth -> ScreenSlideDirection.LEFT
    else -> ScreenSlideDirection.NONE
}

// ===== MENUS =====

fun menuEnter(mode: AnimationMode): EnterTransition = when (mode) {
    AnimationMode.ANIM_1 -> fadeIn(tween(180)) + scaleIn(
        initialScale = 0.85f,
        transformOrigin = TransformOrigin(1f, 0f),
        animationSpec = tween(180)
    )
    AnimationMode.ANIM_2 -> slideInHorizontally(
        animationSpec = tween(280)
    ) { fullWidth -> fullWidth } + fadeIn(tween(280))
    AnimationMode.NONE -> EnterTransition.None
}

fun menuExit(mode: AnimationMode): ExitTransition = when (mode) {
    AnimationMode.ANIM_1 -> fadeOut(tween(140)) + scaleOut(
        targetScale = 0.85f,
        transformOrigin = TransformOrigin(1f, 0f),
        animationSpec = tween(140)
    )
    AnimationMode.ANIM_2 -> slideOutHorizontally(
        animationSpec = tween(220)
    ) { fullWidth -> fullWidth } + fadeOut(tween(220))
    AnimationMode.NONE -> ExitTransition.None
}

// ===== BOTTOM BARS =====

fun bottomBarEnter(mode: AnimationMode): EnterTransition = when (mode) {
    AnimationMode.ANIM_1 -> slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(220)
    ) + fadeIn(tween(220))
    AnimationMode.ANIM_2 -> slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(280)
    ) + fadeIn(tween(280))
    AnimationMode.NONE -> EnterTransition.None
}

fun bottomBarExit(mode: AnimationMode): ExitTransition = when (mode) {
    AnimationMode.ANIM_1 -> slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(180)
    ) + fadeOut(tween(180))
    AnimationMode.ANIM_2 -> slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(220)
    ) + fadeOut(tween(220))
    AnimationMode.NONE -> ExitTransition.None
}

// ===== SCREEN TRANSITIONS =====

fun screenEnter(
    mode: AnimationMode,
    direction: ScreenSlideDirection
): EnterTransition = when (mode) {
    AnimationMode.ANIM_1 -> fadeIn(tween(200))
    AnimationMode.ANIM_2 -> when (direction) {
        ScreenSlideDirection.RIGHT -> slideInHorizontally(
            animationSpec = tween(320)
        ) { fullWidth -> fullWidth } + fadeIn(tween(320))
        ScreenSlideDirection.LEFT -> slideInHorizontally(
            animationSpec = tween(320)
        ) { fullWidth -> -fullWidth } + fadeIn(tween(320))
        ScreenSlideDirection.NONE -> fadeIn(tween(200))
    }
    AnimationMode.NONE -> EnterTransition.None
}

fun screenExit(
    mode: AnimationMode,
    direction: ScreenSlideDirection
): ExitTransition = when (mode) {
    AnimationMode.ANIM_1 -> fadeOut(tween(160))
    AnimationMode.ANIM_2 -> when (direction) {
        ScreenSlideDirection.RIGHT -> slideOutHorizontally(
            animationSpec = tween(280)
        ) { fullWidth -> -fullWidth / 3 } + fadeOut(tween(280))
        ScreenSlideDirection.LEFT -> slideOutHorizontally(
            animationSpec = tween(280)
        ) { fullWidth -> fullWidth / 3 } + fadeOut(tween(280))
        ScreenSlideDirection.NONE -> fadeOut(tween(160))
    }
    AnimationMode.NONE -> ExitTransition.None
}