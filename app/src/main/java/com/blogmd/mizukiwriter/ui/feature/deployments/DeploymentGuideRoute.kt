package com.blogmd.mizukiwriter.ui.feature.deployments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.deployment.DeploymentEducationCatalog
import com.blogmd.mizukiwriter.ui.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeploymentGuideRoute(
    onBack: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: DeploymentSettingsViewModel = viewModel(
        factory = DeploymentSettingsViewModel.factory(container.settingsRepository),
    )
    val state by viewModel.uiState.collectAsState()
    val article = DeploymentEducationCatalog.guideFor(state.deploymentPlatform)
    val uriHandler = LocalUriHandler.current

    ScaffoldWithTopBar(
        title = "部署教程",
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(article.title, style = MaterialTheme.typography.titleLarge)
                        Text("当前平台：${state.deploymentPlatform.label}", style = MaterialTheme.typography.bodySmall)
                        Text(article.intro, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(article.sections.size) { index ->
                val section = article.sections[index]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Text(section.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("官方文档与跳转", style = MaterialTheme.typography.titleMedium)
                        article.referenceLinks.forEach { link ->
                            TextButton(onClick = { uriHandler.openUri(link.url) }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Text(link.label, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldWithTopBar(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        content = content,
    )
}
