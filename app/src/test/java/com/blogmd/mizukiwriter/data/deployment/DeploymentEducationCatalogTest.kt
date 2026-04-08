package com.blogmd.mizukiwriter.data.deployment

import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeploymentEducationCatalogTest {

    @Test
    fun `catalog exposes guide sections for every deployment platform`() {
        DeploymentPlatform.entries.forEach { platform ->
            val guide = DeploymentEducationCatalog.guideFor(platform)

            assertThat(guide.title).isNotEmpty()
            assertThat(guide.sections).isNotEmpty()
            assertThat(guide.referenceLinks).isNotEmpty()
        }
    }

    @Test
    fun `catalog exposes field docs for every deployment platform`() {
        DeploymentPlatform.entries.forEach { platform ->
            val docs = DeploymentEducationCatalog.fieldDocsFor(platform)

            assertThat(docs).isNotEmpty()
            assertThat(docs.all { doc -> doc.label.isNotBlank() && doc.help.isNotBlank() }).isTrue()
            assertThat(docs.all { doc -> doc.referenceUrl.isNotBlank() }).isTrue()
        }
    }
}
