package me.weishu.kernelsu.cogan

import me.weishu.kernelsu.ui.UiMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CoganPresentationTest {
    @Test
    fun bridgeReleaseWinsOverDisguisedUname() {
        val disguised = "6.1.138-android14"
        val kernel = CoganKernelInfo(
            state = CoganKernelState.Detected,
            release = "6.1.176-Cogan-Rothko-ACK-r38",
        )
        assertEquals(kernel.release, kernel.displayRelease(disguised))
        assertEquals(disguised, CoganKernelInfo.Unknown.displayRelease(disguised))
        assertEquals(disguised, CoganKernelInfo.NotCogan.displayRelease(disguised))
        assertEquals(disguised, kernel.copy(release = " ").displayRelease(disguised))
    }

    @Test
    fun allStyleChoicesResolveToExistingScreens() {
        assertEquals(listOf("masculine", "material", "miuix"), UiMode.STYLE_VALUES)
        assertEquals(UiMode.Material, UiMode.fromValue(UiMode.MASCULINE_VALUE))
        assertEquals(UiMode.Material, UiMode.fromValue("material"))
        assertEquals(UiMode.Miuix, UiMode.fromValue("miuix"))
        assertEquals(UiMode.Miuix, UiMode.fromValue("unknown"))
    }
}
