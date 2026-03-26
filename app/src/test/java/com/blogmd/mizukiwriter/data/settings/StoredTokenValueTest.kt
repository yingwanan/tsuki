package com.blogmd.mizukiwriter.data.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StoredTokenValueTest {
    @Test
    fun `decode returns plaintext for legacy token values`() {
        val codec = FakeTokenCodec()

        val decoded = decodeStoredTokenValue("ghp_plaintext", codec)

        assertThat(decoded).isEqualTo("ghp_plaintext")
        assertThat(codec.decryptCalls).isEqualTo(0)
    }

    @Test
    fun `encode prefixes encrypted token payload`() {
        val codec = FakeTokenCodec()

        val encoded = encodeStoredTokenValue("ghp_secret", codec)

        assertThat(encoded).isEqualTo("enc:v1:encrypted-ghp_secret")
    }

    @Test
    fun `decode decrypts prefixed token payload`() {
        val codec = FakeTokenCodec()

        val decoded = decodeStoredTokenValue("enc:v1:encrypted-ghp_secret", codec)

        assertThat(decoded).isEqualTo("ghp_secret")
        assertThat(codec.decryptCalls).isEqualTo(1)
    }

    private class FakeTokenCodec : TokenCodec {
        var decryptCalls = 0

        override fun encrypt(value: String): String = "encrypted-$value"

        override fun decrypt(value: String): String {
            decryptCalls++
            return value.removePrefix("encrypted-")
        }
    }
}
