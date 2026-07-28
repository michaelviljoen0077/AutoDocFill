package com.autodocfill.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt initialization
 */
@HiltAndroidApp
class AutoDocFillApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide services here
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cleanup
    }
}
