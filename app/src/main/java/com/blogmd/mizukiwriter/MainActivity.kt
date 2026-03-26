package com.blogmd.mizukiwriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.blogmd.mizukiwriter.ui.MizukiWriterApp
import com.blogmd.mizukiwriter.ui.theme.MizukiWriterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MizukiWriterTheme {
                MizukiWriterApp()
            }
        }
    }
}
