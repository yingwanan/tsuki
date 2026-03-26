package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlugGeneratorTest {

    @Test
    fun `creates kebab slug from english title`() {
        val slug = SlugGenerator.fromTitle("My First Blog Post")

        assertThat(slug).isEqualTo("my-first-blog-post")
    }

    @Test
    fun `removes special characters and trims separators`() {
        val slug = SlugGenerator.fromTitle("  Kotlin & Compose!!!  ")

        assertThat(slug).isEqualTo("kotlin-compose")
    }

    @Test
    fun `falls back to post prefix when title has no ascii letters`() {
        val slug = SlugGenerator.fromTitle("中文标题")

        assertThat(slug).matches("post-[0-9]{8}-[0-9]{6}")
    }
}
