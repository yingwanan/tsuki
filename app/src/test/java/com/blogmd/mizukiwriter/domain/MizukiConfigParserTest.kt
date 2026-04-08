package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MizukiConfigParserTest {

    @Test
    fun `parse extracts primary site settings from mizuki config`() {
        val snapshot = MizukiConfigParser.parse(
            """
            const SITE_LANG = "ja";
            const SITE_TIMEZONE = 8;
            export const siteConfig = {
                title: "Mizuki",
                subtitle: "One demo website",
                siteURL: "https://mizuki.mysqil.com/",
                timeZone: SITE_TIMEZONE,
                lang: SITE_LANG,
            };
            """.trimIndent(),
        )

        assertThat(snapshot.title).isEqualTo("Mizuki")
        assertThat(snapshot.subtitle).isEqualTo("One demo website")
        assertThat(snapshot.siteUrl).isEqualTo("https://mizuki.mysqil.com/")
        assertThat(snapshot.lang).isEqualTo("ja")
        assertThat(snapshot.timeZone).isEqualTo(8)
    }

    @Test
    fun `update rewrites title subtitle site url lang and timezone`() {
        val updated = MizukiConfigParser.update(
            source = """
            const SITE_LANG = "ja";
            const SITE_TIMEZONE = 8;
            export const siteConfig = {
                title: "Mizuki",
                subtitle: "One demo website",
                siteURL: "https://mizuki.mysqil.com/",
                timeZone: SITE_TIMEZONE,
                lang: SITE_LANG,
            };
            """.trimIndent(),
            snapshot = SiteConfigSnapshot(
                title = "Tsuki Console",
                subtitle = "Blog operations suite",
                siteUrl = "https://blog.example.com/",
                lang = "zh_CN",
                timeZone = 9,
            ),
        )

        assertThat(updated).contains("""const SITE_LANG = "zh_CN";""")
        assertThat(updated).contains("""const SITE_TIMEZONE = 9;""")
        assertThat(updated).contains("""title: "Tsuki Console"""")
        assertThat(updated).contains("""subtitle: "Blog operations suite"""")
        assertThat(updated).contains("""siteURL: "https://blog.example.com/"""")
    }
}
