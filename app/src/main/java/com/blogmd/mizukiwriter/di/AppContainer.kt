package com.blogmd.mizukiwriter.di

import android.content.Context
import androidx.room.Room
import com.blogmd.mizukiwriter.data.github.GitHubPublisher
import com.blogmd.mizukiwriter.data.local.MizukiWriterDatabase
import com.blogmd.mizukiwriter.data.media.AssetStorage
import com.blogmd.mizukiwriter.data.repository.DraftRepository
import com.blogmd.mizukiwriter.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        MizukiWriterDatabase::class.java,
        "mizuki-writer.db",
    )
        .addMigrations(MizukiWriterDatabase.MIGRATION_1_2)
        .build()

    val draftRepository = DraftRepository(database.draftPostDao())
    val settingsRepository = SettingsRepository(context)
    val assetStorage = AssetStorage(context.filesDir)
    val gitHubPublisher = GitHubPublisher(assetStorage)
}
