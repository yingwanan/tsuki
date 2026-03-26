package com.blogmd.mizukiwriter.ui.feature.editor

import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditorDraftMergeTest {
    @Test
    fun `mergeDraftForUi prefers local content and repo sync status`() {
        val repo = DraftPost(
            id = 9L,
            title = "repo title",
            body = "repo body",
            description = "repo description",
            slug = "published-post",
            modifiedAt = 200L,
            publishState = PublishState.Synced,
            lastPublishError = "",
        )
        val local = DraftPost(
            id = 9L,
            title = "local title",
            body = "local body",
            description = "local description",
            slug = "",
            modifiedAt = 100L,
            publishState = PublishState.LocalOnly,
            lastPublishError = "stale",
        )

        val merged = mergeDraftForUi(repo = repo, local = local)
        assertThat(merged).isNotNull()
        val resolved = merged!!

        assertThat(resolved.title).isEqualTo("local title")
        assertThat(resolved.body).isEqualTo("local body")
        assertThat(resolved.description).isEqualTo("local description")
        assertThat(resolved.slug).isEqualTo("published-post")
        assertThat(resolved.publishState).isEqualTo(PublishState.Synced)
        assertThat(resolved.modifiedAt).isEqualTo(200L)
        assertThat(resolved.lastPublishError).isEmpty()
    }

    @Test
    fun `mergeDraftForUi returns available draft when only one side exists`() {
        val repo = DraftPost(id = 1L, title = "repo")
        val local = DraftPost(id = 1L, title = "local")

        assertThat(mergeDraftForUi(repo = repo, local = null)?.title).isEqualTo("repo")
        assertThat(mergeDraftForUi(repo = null, local = local)?.title).isEqualTo("local")
    }
}
