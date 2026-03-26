package com.blogmd.mizukiwriter.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SlugGenerator {
    private val invalidChars = Regex("[^a-z0-9]+")
    private val duplicateHyphen = Regex("-+")
    private val fallbackFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)

    fun fromTitle(title: String, now: LocalDateTime = LocalDateTime.now()): String {
        val normalized = title
            .lowercase(Locale.US)
            .trim()
            .replace(invalidChars, "-")
            .replace(duplicateHyphen, "-")
            .trim('-')

        if (normalized.isNotBlank()) {
            return normalized
        }

        return "post-${now.format(fallbackFormatter)}"
    }
}
