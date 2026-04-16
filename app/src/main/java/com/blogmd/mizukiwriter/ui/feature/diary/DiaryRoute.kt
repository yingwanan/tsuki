package com.blogmd.mizukiwriter.ui.feature.diary

import androidx.compose.runtime.Composable
import com.blogmd.mizukiwriter.domain.MizukiCatalog
import com.blogmd.mizukiwriter.ui.feature.repositoryfile.RepositoryFileRoute

@Composable
fun DiaryRoute() {
    RepositoryFileRoute(
        path = MizukiCatalog.diaryPath,
        title = "日记",
        bindingName = MizukiCatalog.diaryBinding,
        onBack = null,
    )
}
