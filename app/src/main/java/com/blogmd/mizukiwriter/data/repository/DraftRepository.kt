package com.blogmd.mizukiwriter.data.repository

import com.blogmd.mizukiwriter.data.local.DraftPostDao
import com.blogmd.mizukiwriter.data.local.toEntity
import com.blogmd.mizukiwriter.data.local.toModel
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DraftRepositoryContract {
    fun observeAll(): Flow<List<DraftPost>>
    fun observeById(id: Long): Flow<DraftPost?>
    suspend fun createBlankDraft(): Long
    suspend fun getById(id: Long): DraftPost?
    suspend fun save(draft: DraftPost): Long
    suspend fun markPublishSuccess(draft: DraftPost, slug: String)
    suspend fun markPublishFailure(draft: DraftPost, message: String)
    suspend fun delete(draftId: Long)
}

class DraftRepository(
    private val dao: DraftPostDao,
) : DraftRepositoryContract {
    override fun observeAll(): Flow<List<DraftPost>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override fun observeById(id: Long): Flow<DraftPost?> = dao.observeById(id).map { it?.toModel() }

    override suspend fun createBlankDraft(): Long {
        val now = System.currentTimeMillis()
        return dao.upsert(
            DraftPost(
                createdAt = now,
                modifiedAt = now,
            ).toEntity(),
        )
    }

    override suspend fun getById(id: Long): DraftPost? = dao.getById(id)?.toModel()

    override suspend fun save(draft: DraftPost): Long = dao.upsert(
        draft.copy(modifiedAt = System.currentTimeMillis()).toEntity(),
    )

    override suspend fun markPublishSuccess(draft: DraftPost, slug: String) {
        save(
            draft.copy(
                slug = slug,
                publishState = PublishState.Synced,
                lastPublishError = "",
            ),
        )
    }

    override suspend fun markPublishFailure(draft: DraftPost, message: String) {
        save(
            draft.copy(
                publishState = PublishState.Failed,
                lastPublishError = message,
            ),
        )
    }

    override suspend fun delete(draftId: Long) {
        dao.getById(draftId)?.let { dao.delete(it) }
    }
}
