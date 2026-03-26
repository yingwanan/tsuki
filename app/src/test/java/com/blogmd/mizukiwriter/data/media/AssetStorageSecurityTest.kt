package com.blogmd.mizukiwriter.data.media

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetStorageSecurityTest {
    @Test
    fun `sanitizeAssetExtension strips unsafe characters and path fragments`() {
        val sanitized = sanitizeAssetExtension("../pn/g?token=1")

        assertThat(sanitized).isEqualTo("png")
    }

    @Test
    fun `sanitizeAssetExtension falls back to bin when extension becomes empty`() {
        val sanitized = sanitizeAssetExtension("..//")

        assertThat(sanitized).isEqualTo("bin")
    }
}
