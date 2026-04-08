package com.blogmd.mizukiwriter.domain

object MizukiConfigParser {
    private val langRegex = Regex("""const\s+SITE_LANG\s*=\s*"([^"]*)"""")
    private val timeZoneRegex = Regex("""const\s+SITE_TIMEZONE\s*=\s*(-?\d+)""")
    private val titleRegex = Regex("""title:\s*"([^"]*)"""")
    private val subtitleRegex = Regex("""subtitle:\s*"([^"]*)"""")
    private val siteUrlRegex = Regex("""siteURL:\s*"([^"]*)"""")

    fun parse(source: String): SiteConfigSnapshot = SiteConfigSnapshot(
        title = titleRegex.find(source)?.groupValues?.get(1).orEmpty(),
        subtitle = subtitleRegex.find(source)?.groupValues?.get(1).orEmpty(),
        siteUrl = siteUrlRegex.find(source)?.groupValues?.get(1).orEmpty(),
        lang = langRegex.find(source)?.groupValues?.get(1).orEmpty(),
        timeZone = timeZoneRegex.find(source)?.groupValues?.get(1)?.toIntOrNull() ?: 8,
    )

    fun update(source: String, snapshot: SiteConfigSnapshot): String = source
        .replace(langRegex, """const SITE_LANG = "${escape(snapshot.lang)}"""")
        .replace(timeZoneRegex, """const SITE_TIMEZONE = ${snapshot.timeZone}""")
        .replace(titleRegex, """title: "${escape(snapshot.title)}"""")
        .replace(subtitleRegex, """subtitle: "${escape(snapshot.subtitle)}"""")
        .replace(siteUrlRegex, """siteURL: "${escape(snapshot.siteUrl)}"""")

    private fun escape(value: String): String = value.replace("\"", "\\\"")
}
