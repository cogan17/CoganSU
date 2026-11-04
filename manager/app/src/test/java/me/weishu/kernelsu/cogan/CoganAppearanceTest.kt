package me.weishu.kernelsu.cogan

import java.lang.reflect.Proxy
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.ThemeController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoganAppearanceTest {
    @Test
    fun coganStyleUsesMaterialAndForcesDarkWithoutReadingSavedPalette() {
        // A Cogan preset must not depend on system mode or old wallpaper preferences.
        val repo = Proxy.newProxyInstance(
            SettingsRepository::class.java.classLoader,
            arrayOf(SettingsRepository::class.java),
        ) { _, method, _ ->
            check(method.name == "getUiMode") { "Unexpected preference: ${method.name}" }
            UiMode.COGAN_VALUE
        } as SettingsRepository
        val settings = ThemeController.getAppSettings(repo)
        assertEquals(UiMode.Material, UiMode.fromValue(UiMode.COGAN_VALUE))
        assertEquals(ColorMode.DARK_AMOLED, settings.colorMode)
        assertTrue(settings.coganMasculine)
        assertEquals(0xFF90CAF9.toInt(), settings.keyColor)
        assertEquals(listOf("miuix", "material", "cogan"), UiMode.styleValues)
        assertEquals(UiMode.Miuix, UiMode.fromValue("unknown"))
    }
}
