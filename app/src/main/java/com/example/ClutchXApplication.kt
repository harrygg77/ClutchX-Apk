package com.example

import android.app.Application
import com.example.notifications.NotificationBackgroundMonitor

class ClutchXApplication : Application() {

    lateinit var notificationMonitor: NotificationBackgroundMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        notificationMonitor = NotificationBackgroundMonitor(applicationContext)
        notificationMonitor.start()
    }
}
