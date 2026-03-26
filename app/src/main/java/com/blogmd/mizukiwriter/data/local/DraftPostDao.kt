package com.blogmd.mizukiwriter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftPostDao {
    @Query("SELECT * FROM draft_posts ORDER BY modifiedAt DESC")
    fun observeAll(): Flow<List<DraftPostEntity>>

    @Query("SELECT * FROM draft_posts WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<DraftPostEntity?>

    @Query("SELECT * FROM draft_posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DraftPostEntity?

    @Upsert
    suspend fun upsert(entity: DraftPostEntity): Long

    @Delete
    suspend fun delete(entity: DraftPostEntity)
}
