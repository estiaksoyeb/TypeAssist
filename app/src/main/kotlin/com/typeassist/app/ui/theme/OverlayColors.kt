package com.typeassist.app.ui.theme

import android.graphics.Color as AndroidColor

/**
 * Overlay palette for WindowManager-hosted views in [com.typeassist.app.service.OverlayManager].
 *
 * These views are built imperatively (android.widget.*) and cannot read
 * MaterialTheme.colorScheme.*. Keep the values below in sync with Color.kt.
 * Every hex corresponds to a role in the M3 scheme:
 *   surface          -> surfaceLight / surfaceDark
 *   onSurface        -> onSurfaceLight / onSurfaceDark
 *   onSurfaceVariant -> onSurfaceVariantLight / onSurfaceVariantDark
 *   primary          -> primaryLight / primaryDark
 */
data class OverlayColors(
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
) {
    companion object {
        fun forDark(dark: Boolean): OverlayColors = if (dark) {
            OverlayColors(
                surface = 0xFF111318.toInt(),
                onSurface = 0xFFE2E2E9.toInt(),
                onSurfaceVariant = 0xFFC4C6D0.toInt(),
                primary = 0xFFAAC7FF.toInt(),
            )
        } else {
            OverlayColors(
                surface = 0xFFF9F9FF.toInt(),
                onSurface = 0xFF191C20.toInt(),
                onSurfaceVariant = 0xFF44474E.toInt(),
                primary = 0xFF415F91.toInt(),
            )
        }
    }
}

@Suppress("unused")
private val _importGuard = AndroidColor.BLACK
