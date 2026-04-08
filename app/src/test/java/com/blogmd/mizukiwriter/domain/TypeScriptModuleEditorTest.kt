package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class TypeScriptModuleEditorTest {

    @Test
    fun `extracts exported object literal as json tree`() {
        val module = TypeScriptModuleEditor.parseExport(
            source = """
            export const siteConfig = {
                title: "Mizuki",
                featurePages: {
                    diary: true,
                    friends: false,
                },
                sidebar: ["profile", "tags"],
            };
            """.trimIndent(),
            exportName = "siteConfig",
        )

        val root = module.value as JsonObject
        assertThat(root["title"]).isEqualTo(JsonPrimitive("Mizuki"))
        assertThat((root["featurePages"] as JsonObject)["diary"]).isEqualTo(JsonPrimitive(true))
        assertThat((root["sidebar"] as JsonArray).size).isEqualTo(2)
    }

    @Test
    fun `updates exported array literal and preserves surrounding module content`() {
        val updated = TypeScriptModuleEditor.updateExport(
            source = """
            import type { DiaryItem } from "./types";

            export const diaryData: DiaryItem[] = [
                {
                    id: 1,
                    content: "hello",
                    tags: ["first"],
                },
            ];

            export const untouched = true;
            """.trimIndent(),
            exportName = "diaryData",
            transform = { current ->
                val array = current as JsonArray
                JsonArray(
                    array + JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(2),
                            "content" to JsonPrimitive("second"),
                            "tags" to JsonArray(listOf(JsonPrimitive("note"), JsonPrimitive("demo"))),
                        ),
                    ),
                )
            },
        )

        assertThat(updated).contains("export const untouched = true;")
        assertThat(updated).contains("content: \"second\"")
        assertThat(updated).contains("tags: [\"note\", \"demo\"]")
    }

    @Test
    fun `parses and preserves typescript identifiers in config exports`() {
        val source = """
            export const siteConfig = {
                lang: SITE_LANG,
                timezone: SITE_TIMEZONE,
                theme: "aurora",
            };

            export const navBarConfig = {
                links: [LinkPreset.Home, LinkPreset.About],
            };
        """.trimIndent()

        val siteConfig = TypeScriptModuleEditor.parseBinding(source, "siteConfig").value as JsonObject
        val navBarConfig = TypeScriptModuleEditor.parseBinding(source, "navBarConfig").value as JsonObject

        assertThat((siteConfig["lang"] as JsonPrimitive).content).isEqualTo("__ts_expr__:SITE_LANG")
        assertThat((siteConfig["timezone"] as JsonPrimitive).content).isEqualTo("__ts_expr__:SITE_TIMEZONE")
        assertThat(((navBarConfig["links"] as JsonArray)[0] as JsonPrimitive).content)
            .isEqualTo("__ts_expr__:LinkPreset.Home")

        val updated = TypeScriptModuleEditor.updateBinding(source, "navBarConfig") { current ->
            val root = current as JsonObject
            JsonObject(
                root.toMutableMap().apply {
                    put(
                        "links",
                        JsonArray(
                            listOf(
                                JsonPrimitive("__ts_expr__:LinkPreset.Home"),
                                JsonPrimitive("__ts_expr__:LinkPreset.Archive"),
                            ),
                        ),
                    )
                },
            )
        }

        assertThat(updated).contains("links: [LinkPreset.Home, LinkPreset.Archive]")
        assertThat(updated).doesNotContain("\"__ts_expr__:LinkPreset.Home\"")
    }

    @Test
    fun `parses strings containing apostrophes and hook keywords without treating them as expressions`() {
        val source = """
            export const animeData = [
                {
                    title: "Lycoris Recoil",
                    description: "Girl's gunfight",
                    tags: ["Hooks, Context, and state"],
                },
            ];
        """.trimIndent()

        val document = TypeScriptModuleEditor.parseBinding(source, "animeData")
        val items = document.value as JsonArray
        val first = items.first() as JsonObject

        assertThat((first["description"] as JsonPrimitive).content).isEqualTo("Girl's gunfight")
        assertThat((first["tags"] as JsonArray).first()).isEqualTo(JsonPrimitive("Hooks, Context, and state"))
    }
}
