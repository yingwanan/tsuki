package com.blogmd.mizukiwriter.domain

import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.WorkspaceMode

data class GitBranchTarget(
    val baseBranch: String,
    val targetBranch: String,
    val requiresBranchCreation: Boolean,
)

object GitBranchTargetResolver {
    fun resolve(
        settings: GitHubSettings,
        generatedBranchName: String,
    ): GitBranchTarget {
        val baseBranch = settings.branch.ifBlank { DEFAULT_BRANCH }
        return when (settings.workspaceMode) {
            WorkspaceMode.DirectCommit -> GitBranchTarget(
                baseBranch = baseBranch,
                targetBranch = baseBranch,
                requiresBranchCreation = false,
            )

            WorkspaceMode.WorkingBranch -> GitBranchTarget(
                baseBranch = baseBranch,
                targetBranch = settings.updateBranch.ifBlank { generatedBranchName },
                requiresBranchCreation = true,
            )
        }
    }

    private const val DEFAULT_BRANCH = "master"
}
