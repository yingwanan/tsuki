package com.blogmd.mizukiwriter.ui.components

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).also { textView ->
                textView.textSize = 16f
                textView.setLineSpacing(0f, 1.25f)
                textView.setPadding(24, 24, 24, 24)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
    )
}
