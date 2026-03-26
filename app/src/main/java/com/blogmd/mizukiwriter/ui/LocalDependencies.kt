package com.blogmd.mizukiwriter.ui

import android.content.Context
import com.blogmd.mizukiwriter.MizukiWriterApplication
import com.blogmd.mizukiwriter.di.AppContainer

val Context.appContainer: AppContainer
    get() = (applicationContext as MizukiWriterApplication).appContainer
