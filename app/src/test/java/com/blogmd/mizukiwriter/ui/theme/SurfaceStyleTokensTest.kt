package com.blogmd.mizukiwriter.ui.theme

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SurfaceStyleTokensTest {
    @Test
    fun `light card surface stays opaque and distinct from background`() {
        assertThat(CardSurfaceLight.alpha).isEqualTo(1f)
        assertThat(CardSurfaceLight).isNotEqualTo(BackgroundLight)
    }

    @Test
    fun `dark card surface stays opaque and distinct from background`() {
        assertThat(CardSurfaceDark.alpha).isEqualTo(1f)
        assertThat(CardSurfaceDark).isNotEqualTo(BackgroundDark)
    }

    @Test
    fun `swipe card mask colors stay fully opaque`() {
        assertThat(SwipeCardMaskLight.alpha).isEqualTo(1f)
        assertThat(SwipeCardMaskDark.alpha).isEqualTo(1f)
    }

    @Test
    fun `dark theme separates primary and secondary text clearly`() {
        assertThat(TextLight.luminance()).isGreaterThan(TextSecondaryLight.luminance())
    }

    @Test
    fun `shared dark glass token stays opaque`() {
        assertThat(GlassDark.alpha).isEqualTo(1f)
    }

    @Test
    fun `app chrome switches between light and dark container colors`() {
        assertThat(appChromeContainerColor(isDarkTheme = false)).isEqualTo(GlassWhite)
        assertThat(appChromeContainerColor(isDarkTheme = true)).isEqualTo(GlassDark)
    }

    @Test
    fun `background image scrim switches with theme`() {
        assertThat(appBackgroundImageScrim(isDarkTheme = false)).isEqualTo(LightBackgroundScrim)
        assertThat(appBackgroundImageScrim(isDarkTheme = true)).isEqualTo(DarkBackgroundScrim)
    }

    @Test
    fun `top bar blends into screen background and keeps compact shared sizing`() {
        assertThat(appTopBarContainerColor(isDarkTheme = false)).isEqualTo(BackgroundLight)
        assertThat(appTopBarContainerColor(isDarkTheme = true)).isEqualTo(BackgroundDark)
        assertThat(appTopBarVerticalPadding).isEqualTo(0.dp)
        assertThat(appTopBarMinHeight).isEqualTo(32.dp)
    }
}
