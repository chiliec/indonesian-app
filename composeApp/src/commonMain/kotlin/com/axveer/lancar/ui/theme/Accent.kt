package com.axveer.lancar.ui.theme

import androidx.compose.ui.graphics.Color

// The three accents offered by the design handoff (Theme.kt palette + blue alt).
val LancarBlue = Color(0xFF31547E)

enum class Accent(val color: Color) {
    TERRACOTTA(LancarTerracotta),
    GREEN(LancarGreen),
    BLUE(LancarBlue);

    companion object {
        fun fromName(name: String?): Accent = entries.firstOrNull { it.name == name } ?: TERRACOTTA
    }
}
