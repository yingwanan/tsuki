package com.blogmd.mizukiwriter

import android.app.Application
import com.blogmd.mizukiwriter.di.AppContainer

class MizukiWriterApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
