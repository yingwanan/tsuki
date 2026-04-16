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
                workspaceMode = preferences[WORKSPACE_MODE]
                    ?.let { runCatching { WorkspaceMode.valueOf(it) }.getOrNull() }
                    ?: GitHubSettings().workspaceMode,
                updateBranch = preferences[UPDATE_BRANCH].orEmpty(),
                postsBasePath = preferences[POSTS_BASE_PATH] ?: "src/content/posts",
                pagesBasePath = preferences[PAGES_BASE_PATH] ?: GitHubSettings().pagesBasePath,
                configPath = preferences[CONFIG_PATH] ?: GitHubSettings().configPath,
                deploymentWorkflow = preferences[DEPLOYMENT_WORKFLOW].orEmpty(),
                personalAccessToken = decodeStoredTokenValue(preferences[TOKEN].orEmpty(), tokenCodec),
                defaultAuthor = preferences[DEFAULT_AUTHOR].orEmpty(),
                defaultLicenseName = preferences[DEFAULT_LICENSE].orEmpty(),
                backgroundImagePath = preferences[BACKGROUND_IMAGE_PATH].orEmpty(),
                deploymentPlatform = preferences[DEPLOYMENT_PLATFORM]
                    ?.let { runCatching { DeploymentPlatform.valueOf(it) }.getOrNull() }
                    ?: GitHubSettings().deploymentPlatform,
                deploymentAccessToken = decodeStoredTokenValue(preferences[DEPLOYMENT_TOKEN].orEmpty(), tokenCodec),
                deploymentProjectName = preferences[DEPLOYMENT_PROJECT_NAME].orEmpty(),
                deploymentProjectId = preferences[DEPLOYMENT_PROJECT_ID].orEmpty(),
                deploymentAccountId = preferences[DEPLOYMENT_ACCOUNT_ID].orEmpty(),
                deploymentTeamId = preferences[DEPLOYMENT_TEAM_ID].orEmpty(),
                deploymentProductionDomain = preferences[DEPLOYMENT_PRODUCTION_DOMAIN].orEmpty(),
                deploymentPreviewDomain = preferences[DEPLOYMENT_PREVIEW_DOMAIN].orEmpty(),
                deploymentCustomDomain = preferences[DEPLOYMENT_CUSTOM_DOMAIN].orEmpty(),
                deploymentCustomDomainStatus = preferences[DEPLOYMENT_CUSTOM_DOMAIN_STATUS].orEmpty(),
                deploymentBuildCommand = preferences[DEPLOYMENT_BUILD_COMMAND] ?: GitHubSettings().deploymentBuildCommand,
                deploymentOutputDirectory = preferences[DEPLOYMENT_OUTPUT_DIRECTORY]
                    ?: GitHubSettings().deploymentOutputDirectory,
                edgeOneExecutionMode = preferences[EDGEONE_EXECUTION_MODE]
                    ?.let { runCatching { EdgeOneExecutionMode.valueOf(it) }.getOrNull() }
                    ?: GitHubSettings().edgeOneExecutionMode,
            )
        }

    override suspend fun save(settings: GitHubSettings) {
        dataStore.edit { preferences ->
            preferences[OWNER] = settings.owner
            preferences[REPO] = settings.repo
            preferences[BRANCH] = settings.branch
            preferences[WORKSPACE_MODE] = settings.workspaceMode.name
            preferences[UPDATE_BRANCH] = settings.updateBranch
            preferences[POSTS_BASE_PATH] = settings.postsBasePath
            preferences[PAGES_BASE_PATH] = settings.pagesBasePath
            preferences[CONFIG_PATH] = settings.configPath
            preferences[DEPLOYMENT_WORKFLOW] = settings.deploymentWorkflow
            preferences[TOKEN] = encodeStoredTokenValue(settings.personalAccessToken, tokenCodec)
            preferences[DEFAULT_AUTHOR] = settings.defaultAuthor
            preferences[DEFAULT_LICENSE] = settings.defaultLicenseName
            preferences[BACKGROUND_IMAGE_PATH] = settings.backgroundImagePath
            preferences[DEPLOYMENT_PLATFORM] = settings.deploymentPlatform.name
            preferences[DEPLOYMENT_TOKEN] = encodeStoredTokenValue(settings.deploymentAccessToken, tokenCodec)
            preferences[DEPLOYMENT_PROJECT_NAME] = settings.deploymentProjectName
            preferences[DEPLOYMENT_PROJECT_ID] = settings.deploymentProjectId
            preferences[DEPLOYMENT_ACCOUNT_ID] = settings.deploymentAccountId
            preferences[DEPLOYMENT_TEAM_ID] = settings.deploymentTeamId
            preferences[DEPLOYMENT_PRODUCTION_DOMAIN] = settings.deploymentProductionDomain
            preferences[DEPLOYMENT_PREVIEW_DOMAIN] = settings.deploymentPreviewDomain
            preferences[DEPLOYMENT_CUSTOM_DOMAIN] = settings.deploymentCustomDomain
            preferences[DEPLOYMENT_CUSTOM_DOMAIN_STATUS] = settings.deploymentCustomDomainStatus
            preferences[DEPLOYMENT_BUILD_COMMAND] = settings.deploymentBuildCommand
            preferences[DEPLOYMENT_OUTPUT_DIRECTORY] = settings.deploymentOutputDirectory
            preferences[EDGEONE_EXECUTION_MODE] = settings.edgeOneExecutionMode.name
        }
    }

    companion object {
        private val OWNER = stringPreferencesKey("owner")
        private val REPO = stringPreferencesKey("repo")
        private val BRANCH = stringPreferencesKey("branch")
        private val WORKSPACE_MODE = stringPreferencesKey("workspace_mode")
        private val UPDATE_BRANCH = stringPreferencesKey("update_branch")
        private val POSTS_BASE_PATH = stringPreferencesKey("posts_base_path")
        private val PAGES_BASE_PATH = stringPreferencesKey("pages_base_path")
        private val CONFIG_PATH = stringPreferencesKey("config_path")
        private val DEPLOYMENT_WORKFLOW = stringPreferencesKey("deployment_workflow")
        private val TOKEN = stringPreferencesKey("token")
        private val DEFAULT_AUTHOR = stringPreferencesKey("default_author")
        private val DEFAULT_LICENSE = stringPreferencesKey("default_license")
        private val BACKGROUND_IMAGE_PATH = stringPreferencesKey("background_image_path")
        private val DEPLOYMENT_PLATFORM = stringPreferencesKey("deployment_platform")
        private val DEPLOYMENT_TOKEN = stringPreferencesKey("deployment_token")
        private val DEPLOYMENT_PROJECT_NAME = stringPreferencesKey("deployment_project_name")
        private val DEPLOYMENT_PROJECT_ID = stringPreferencesKey("deployment_project_id")
        private val DEPLOYMENT_ACCOUNT_ID = stringPreferencesKey("deployment_account_id")
        private val DEPLOYMENT_TEAM_ID = stringPreferencesKey("deployment_team_id")
        private val DEPLOYMENT_PRODUCTION_DOMAIN = stringPreferencesKey("deployment_production_domain")
        private val DEPLOYMENT_PREVIEW_DOMAIN = stringPreferencesKey("deployment_preview_domain")
        private val DEPLOYMENT_CUSTOM_DOMAIN = stringPreferencesKey("deployment_custom_domain")
        private val DEPLOYMENT_CUSTOM_DOMAIN_STATUS = stringPreferencesKey("deployment_custom_domain_status")
        private val DEPLOYMENT_BUILD_COMMAND = stringPreferencesKey("deployment_build_command")
        private val DEPLOYMENT_OUTPUT_DIRECTORY = stringPreferencesKey("deployment_output_directory")
        private val EDGEONE_EXECUTION_MODE = stringPreferencesKey("edgeone_execution_mode")
    }
}
