package com.blogmd.mizukiwriter.ui.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditorFieldValueSyncTest {
    @Test
    fun `syncExternalTextFieldValue keeps cursor position when text changes externally`() {
        val current = TextFieldValue(
            text = "published",
            selection = TextRange(4, 4),
        )

        val synced = syncExternalTextFieldValue(
            current = current,
            externalText = "published today",
        )

        assertThat(synced.text).isEqualTo("published today")
        assertThat(synced.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun `syncExternalTextFieldValue clamps cursor when new text is shorter`() {
        val current = TextFieldValue(
            text = "description",
            selection = TextRange(11, 11),
        )

        val synced = syncExternalTextFieldValue(
            current = current,
            externalText = "desc",
        )

        assertThat(synced.text).isEqualTo("desc")
        assertThat(synced.selection).isEqualTo(TextRange(4, 4))
    }
}
