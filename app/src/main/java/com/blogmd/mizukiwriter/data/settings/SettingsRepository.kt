package com.blogmd.mizukiwriter.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepositoryContract {
    val settings: Flow<GitHubSettings>

    suspend fun save(settings: GitHubSettings)
}

class SettingsRepository(context: Context) : SettingsRepositoryContract {
    private val tokenCodec: TokenCodec = AndroidKeyStoreTokenCodec()
    private val dataStore: DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("settings.preferences_pb") },
    )

    override val settings: Flow<GitHubSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            GitHubSettings(
                owner = preferences[OWNER].orEmpty(),
                repo = preferences[REPO].orEmpty(),
                branch = preferences[BRANCH] ?: GitHubSettings().branch,
                postsBasePath = preferences[POSTS_BASE_PATH] ?: "src/content/posts",
                personalAccessToken = decodeStoredTokenValue(preferences[TOKEN].orEmpty(), tokenCodec),
                defaultAuthor = preferences[DEFAULT_AUTHOR].orEmpty(),
                defaultLicenseName = preferences[DEFAULT_LICENSE].orEmpty(),
            )
        }

    override suspend fun save(settings: GitHubSettings) {
        dataStore.edit { preferences ->
            preferences[OWNER] = settings.owner
            preferences[REPO] = settings.repo
            preferences[BRANCH] = settings.branch
            preferences[POSTS_BASE_PATH] = settings.postsBasePath
            preferences[TOKEN] = encodeStoredTokenValue(settings.personalAccessToken, tokenCodec)
            preferences[DEFAULT_AUTHOR] = settings.defaultAuthor
            preferences[DEFAULT_LICENSE] = settings.defaultLicenseName
        }
    }

    companion object {
        private val OWNER = stringPreferencesKey("owner")
        private val REPO = stringPreferencesKey("repo")
        private val BRANCH = stringPreferencesKey("branch")
        private val POSTS_BASE_PATH = stringPreferencesKey("posts_base_path")
        private val TOKEN = stringPreferencesKey("token")
        private val DEFAULT_AUTHOR = stringPreferencesKey("default_author")
        private val DEFAULT_LICENSE = stringPreferencesKey("default_license")
    }
}
