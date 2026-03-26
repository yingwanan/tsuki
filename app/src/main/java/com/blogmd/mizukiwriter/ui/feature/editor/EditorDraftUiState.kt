package com.blogmd.mizukiwriter.ui.feature.editor

import com.blogmd.mizukiwriter.data.model.DraftPost

internal fun mergeDraftForUi(
    repo: DraftPost?,
    local: DraftPost?,
): DraftPost? {
    if (repo == null) return local
    if (local == null || local.id != repo.id) return repo
    return local.copy(
        id = repo.id,
        slug = repo.slug,
        createdAt = repo.createdAt,
        modifiedAt = repo.modifiedAt,
        publishState = repo.publishState,
        lastPublishError = repo.lastPublishError,
    )
}
