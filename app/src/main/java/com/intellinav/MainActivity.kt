package com.intellinav

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.intellinav.ui.navigation.AppNavigation
import com.intellinav.ui.theme.INTELLINAVTheme

class MyApplication : Application() {
    companion object {
        lateinit var appContext: android.content.Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permissions are now requested during onboarding/profile setup

        setContent {
            INTELLINAVTheme {
                AppNavigation()
            }
        }
    }
}
