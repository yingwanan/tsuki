package com.blogmd.mizukiwriter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.blogmd.mizukiwriter.data.model.PublishState

@Entity(tableName = "draft_posts")
data class DraftPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val slug: String = "",
    val body: String = "",
    val description: String = "",
    val published: String = "",
    val updated: String = "",
    val date: String = "",
    val pubDate: String = "",
    val tagsJson: String = "[]",
    val aliasJson: String = "[]",
    val category: String = "",
    val lang: String = "",
    val draft: Boolean = false,
    val pinned: Boolean = false,
    val comment: Boolean = true,
    val priority: Int = 0,
    val image: String = "",
    val author: String = "",
    val sourceLink: String = "",
    val licenseName: String = "",
    val licenseUrl: String = "",
    val permalink: String = "",
    val encrypted: Boolean = false,
    val password: String = "",
    val passwordHint: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val publishState: PublishState = PublishState.LocalOnly,
    val lastPublishError: String = "",
)
