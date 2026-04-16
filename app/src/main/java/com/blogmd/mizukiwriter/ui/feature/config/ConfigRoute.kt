package com.blogmd.mizukiwriter.ui.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@Composable
fun ConfigRoute() {
    val container = LocalContext.current.appContainer
    val viewModel: ConfigViewModel = viewModel(
        factory = ConfigViewModel.factory(
            settingsRepository = container.settingsRepository,
            workspaceRepository = container.gitHubWorkspaceRepository,
        )
)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    var title by remember(state.snapshot) { mutableStateOf(state.snapshot.title) }
    var subtitle by remember(state.snapshot) { mutableStateOf(state.snapshot.subtitle) }
    var siteUrl by remember(state.snapshot) { mutableStateOf(state.snapshot.siteUrl) }
    var lang by remember(state.snapshot) { mutableStateOf(state.snapshot.lang) }
    var timeZone by remember(state.snapshot) { mutableStateOf(state.snapshot.timeZone.toString()) }
    var rawSource by remember(state.rawSource) { mutableStateOf(state.rawSource) }

    PrimaryScreenScaffold(title = "站点配置") { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("结构化配置", style = MaterialTheme.typography.titleMedium)
                        ConfigField("标题", title) { title = it }
                        ConfigField("副标题", subtitle) { subtitle = it }
                        ConfigField("站点 URL", siteUrl) { siteUrl = it }
                        ConfigField("语言", lang) { lang = it }
                        ConfigField("时区", timeZone) { timeZone = it }
                        Button(
                            onClick = {
                                viewModel.updateRawSource(rawSource)
                                viewModel.save(
                                    SiteConfigSnapshot(
                                        title = title,
                                        subtitle = subtitle,
                                        siteUrl = siteUrl,
                                        lang = lang,
                                        timeZone = timeZone.toIntOrNull() ?: 8,
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存配置")
                        }
                    }
                }
            }
            item {
                Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("高级源码", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = rawSource,
                            onValueChange = {
                                rawSource = it
                                viewModel.updateRawSource(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            minLines = 10,
                        )
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
)
}
