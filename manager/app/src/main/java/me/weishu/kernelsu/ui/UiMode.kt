package me.weishu.kernelsu.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value, COGAN_VALUE -> Material // CoganSU: reuse Material components.
            else -> Miuix
        }

        // CoganSU: a visual preset, not a third copy of every screen.
        const val COGAN_VALUE = "cogan"
        val styleValues = listOf(Miuix.value, Material.value, COGAN_VALUE)
        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
