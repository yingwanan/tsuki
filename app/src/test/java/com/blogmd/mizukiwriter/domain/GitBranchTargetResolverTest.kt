package com.blogmd.mizukiwriter.domain

import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.WorkspaceMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GitBranchTargetResolverTest {

    @Test
    fun `direct commit mode keeps target branch on configured branch`() {
        val target = GitBranchTargetResolver.resolve(
            GitHubSettings(
                branch = "main",
                workspaceMode = WorkspaceMode.DirectCommit,
                updateBranch = "tsuki/update/blog-console-suite",
            ),
            generatedBranchName = "tsuki/update/generated",
        )

        assertThat(target.baseBranch).isEqualTo("main")
        assertThat(target.targetBranch).isEqualTo("main")
        assertThat(target.requiresBranchCreation).isFalse()
    }

    @Test
    fun `working branch mode uses explicit update branch when provided`() {
        val target = GitBranchTargetResolver.resolve(
            GitHubSettings(
                branch = "master",
                workspaceMode = WorkspaceMode.WorkingBranch,
                updateBranch = "tsuki/update/blog-console-suite",
            ),
            generatedBranchName = "tsuki/update/generated",
        )

        assertThat(target.baseBranch).isEqualTo("master")
        assertThat(target.targetBranch).isEqualTo("tsuki/update/blog-console-suite")
        assertThat(target.requiresBranchCreation).isTrue()
    }

    @Test
    fun `working branch mode falls back to generated branch name`() {
        val target = GitBranchTargetResolver.resolve(
            GitHubSettings(
                branch = "",
                workspaceMode = WorkspaceMode.WorkingBranch,
                updateBranch = "",
            ),
            generatedBranchName = "tsuki/update/20260405-blog-console-suite",
        )

        assertThat(target.baseBranch).isEqualTo("master")
        assertThat(target.targetBranch).isEqualTo("tsuki/update/20260405-blog-console-suite")
        assertThat(target.requiresBranchCreation).isTrue()
    }
}
