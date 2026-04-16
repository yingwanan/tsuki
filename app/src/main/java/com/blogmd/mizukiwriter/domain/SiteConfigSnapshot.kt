package com.blogmd.mizukiwriter.domain

data class SiteConfigSnapshot(
    val title: String = "",
    val subtitle: String = "",
    val siteUrl: String = "",
    val lang: String = "",
    val timeZone: Int = 8,
)
