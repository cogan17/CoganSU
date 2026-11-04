package me.weishu.kernelsu.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value, MASCULINE_VALUE -> Material
            else -> Miuix
        }

        // CoganSU: one extra visual preset, reusing the Material screens.
        const val MASCULINE_VALUE = "masculine"
        val STYLE_VALUES = listOf(MASCULINE_VALUE, Material.value, Miuix.value)
        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
