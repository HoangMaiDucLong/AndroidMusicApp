package com.example.musicapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PlayMusicApp : Application() {
    companion object {
        const val channelId : String = "_PlayMusicApp"
        const val channelName: String = "PopoposMP3"
    }

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
    }

    private fun setupNotificationChannel() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).also { channel ->
                channel.setSound(null, null)
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                    createNotificationChannel(channel)
                }
            }
        }
    }
}
