package com.blogmd.mizukiwriter.data.local

import androidx.room.TypeConverter
import com.blogmd.mizukiwriter.data.model.PublishState

class PublishStateConverters {
    @TypeConverter
    fun fromPublishState(value: PublishState): String = value.name

    @TypeConverter
    fun toPublishState(value: String): PublishState = runCatching {
        PublishState.valueOf(value)
    }.getOrDefault(PublishState.LocalOnly)
}
