package com.blogmd.mizukiwriter.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface TokenCodec {
    fun encrypt(value: String): String
    fun decrypt(value: String): String
}

internal fun encodeStoredTokenValue(value: String, codec: TokenCodec): String {
    if (value.isBlank()) return ""
    return "$ENCRYPTED_TOKEN_PREFIX${codec.encrypt(value)}"
}

internal fun decodeStoredTokenValue(value: String, codec: TokenCodec): String {
    if (value.isBlank()) return ""
    if (!value.startsWith(ENCRYPTED_TOKEN_PREFIX)) return value
    return runCatching {
        codec.decrypt(value.removePrefix(ENCRYPTED_TOKEN_PREFIX))
    }.getOrDefault("")
}

internal class AndroidKeyStoreTokenCodec(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : TokenCodec {
    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(UTF_8))
        val iv = Base64.getEncoder().encodeToString(cipher.iv)
        val payload = Base64.getEncoder().encodeToString(encrypted)
        return "$iv:$payload"
    }

    override fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2) { "Invalid encrypted token payload" }
        val iv = Base64.getDecoder().decode(parts[0])
        val payload = Base64.getDecoder().decode(parts[1])
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(payload), UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val DEFAULT_KEY_ALIAS = "blogmd.github_pat"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

private const val ENCRYPTED_TOKEN_PREFIX = "enc:v1:"
