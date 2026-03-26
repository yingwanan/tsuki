package com.blogmd.mizukiwriter.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DraftPostEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(PublishStateConverters::class)
abstract class MizukiWriterDatabase : RoomDatabase() {
    abstract fun draftPostDao(): DraftPostDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE draft_posts ADD COLUMN date TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE draft_posts ADD COLUMN pubDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE draft_posts ADD COLUMN aliasJson TEXT NOT NULL DEFAULT '[]'")
                database.execSQL("ALTER TABLE draft_posts ADD COLUMN lang TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE draft_posts ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
