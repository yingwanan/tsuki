package com.blogmd.mizukiwriter.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.blogmd.mizukiwriter.domain.MarkdownAction

enum class MarkdownEditorMode {
    Edit,
    Split,
    Preview,
}

@Composable
fun MarkdownEditorWorkspace(
    mode: MarkdownEditorMode,
    onModeSelected: (MarkdownEditorMode) -> Unit,
    bodyValue: TextFieldValue,
    onBodyValueChange: (TextFieldValue) -> Unit,
    previewMarkdown: String,
    modifier: Modifier = Modifier,
    editPlaceholder: String = "开始写正文…",
    splitPlaceholder: String = "正文输入区在下方，靠近键盘更方便。",
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarkdownModeSelector(
            mode = mode,
            onModeSelected = onModeSelected,
        )

        when (mode) {
            MarkdownEditorMode.Edit -> {
                OutlinedTextField(
                    value = bodyValue,
                    onValueChange = onBodyValueChange,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text(editPlaceholder) },
                )
            }

            MarkdownEditorMode.Split -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        MarkdownPreview(
                            markdown = previewMarkdown,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        OutlinedTextField(
                            value = bodyValue,
                            onValueChange = onBodyValueChange,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = { Text(splitPlaceholder) },
                        )
                    }
                }
            }

            MarkdownEditorMode.Preview -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MarkdownPreview(
                        markdown = previewMarkdown,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownModeSelector(
    mode: MarkdownEditorMode,
    onModeSelected: (MarkdownEditorMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { onModeSelected(MarkdownEditorMode.Edit) },
            label = { Text(if (mode == MarkdownEditorMode.Edit) "全屏编辑" else "编辑") },
        )
        AssistChip(
            onClick = { onModeSelected(MarkdownEditorMode.Split) },
            label = { Text("分屏") },
        )
        AssistChip(
            onClick = { onModeSelected(MarkdownEditorMode.Preview) },
            label = { Text("预览") },
        )
    }
}

@Composable
fun MarkdownEditorToolbar(
    modifier: Modifier = Modifier,
    onAction: (MarkdownAction) -> Unit,
    onPickImage: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarkdownToolbarChip("粗体", Icons.Outlined.FormatBold) { onAction(MarkdownAction.Bold) }
        MarkdownToolbarChip("斜体", Icons.Outlined.FormatItalic) { onAction(MarkdownAction.Italic) }
        MarkdownToolbarChip("删除线", Icons.Outlined.StrikethroughS) { onAction(MarkdownAction.Strikethrough) }
        MarkdownToolbarChip("标题", Icons.AutoMirrored.Outlined.ShortText) { onAction(MarkdownAction.Heading(2)) }
        MarkdownToolbarChip("引用", Icons.Outlined.FormatQuote) { onAction(MarkdownAction.Quote) }
        MarkdownToolbarChip("列表", Icons.AutoMirrored.Outlined.FormatListBulleted) { onAction(MarkdownAction.BulletList) }
        MarkdownToolbarChip("编号", Icons.Outlined.FormatListNumbered) { onAction(MarkdownAction.OrderedList) }
        MarkdownToolbarChip("任务", Icons.Outlined.Checklist) { onAction(MarkdownAction.TaskList) }
        MarkdownToolbarChip("行内代码", Icons.Outlined.DragHandle) { onAction(MarkdownAction.InlineCode) }
        MarkdownToolbarChip("代码块", Icons.Outlined.Code) { onAction(MarkdownAction.CodeBlock("markdown")) }
        MarkdownToolbarChip("链接", Icons.Outlined.Link) { onAction(MarkdownAction.Link("label", "https://")) }
        onPickImage?.let { imageAction ->
            MarkdownToolbarChip("图片", Icons.Outlined.Image, imageAction)
        }
        MarkdownToolbarChip("居中", Icons.Outlined.Preview) { onAction(MarkdownAction.CenterBlock) }
        MarkdownToolbarChip("分割线", Icons.Outlined.HorizontalRule) { onAction(MarkdownAction.Divider) }
    }
}

@Composable
private fun MarkdownToolbarChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.width(18.dp),
            )
        },
    )
}
