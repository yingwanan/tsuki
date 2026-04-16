package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class MizukiCatalogTemplateTest {

    @Test
    fun `projects template includes complete recommended fields for new entries`() {
        val template = MizukiCatalog.createArrayItemTemplate(
            path = "src/data/projects.ts",
            bindingName = "projectsData",
        ) as JsonObject

        assertThat(template.keys).containsAtLeast(
            "name",
            "description",
            "cover",
            "url",
            "github",
            "techStack",
            "longDescription",
        )
        assertThat(template["techStack"]).isEqualTo(JsonArray(emptyList()))
        assertThat(template["longDescription"]).isEqualTo(JsonPrimitive(""))
    }

    @Test
    fun `project document labels cover newly added recommended fields`() {
        assertThat(
            MizukiCatalog.resolveFieldLabel(
                path = "src/data/projects.ts",
                bindingName = "projectsData",
                key = "cover",
            ),
        ).isEqualTo("项目封面")
        assertThat(
            MizukiCatalog.resolveFieldLabel(
                path = "src/data/projects.ts",
                bindingName = "projectsData",
                key = "github",
            ),
        ).isEqualTo("GitHub 地址")
        assertThat(
            MizukiCatalog.resolveFieldLabel(
                path = "src/data/projects.ts",
                bindingName = "projectsData",
                key = "techStack",
            ),
        ).isEqualTo("技术栈")
        assertThat(
            MizukiCatalog.resolveFieldLabel(
                path = "src/data/projects.ts",
                bindingName = "projectsData",
                key = "longDescription",
            ),
        ).isEqualTo("详细描述")
    }
}
