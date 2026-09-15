package com.sch.sekolah_mobile_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sch.sekolah_mobile_app.data.storage.AndroidPlatformContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatformContext.init(this)
        AndroidPlatformContext.setActivity(this)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}