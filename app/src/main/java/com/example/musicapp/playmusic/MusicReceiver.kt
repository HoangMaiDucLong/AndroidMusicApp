package com.example.musicapp.playmusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.musicapp.Constants.BUNDLE_KEY
import com.example.musicapp.Constants.MUSIC_ACTION_KEY

class MusicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getBundleExtra(BUNDLE_KEY)?.also { receivedBundle ->
            Intent(context, PlayMusicService::class.java).also { intent ->
                val newBundle = Bundle().apply {
                    putSerializable(MUSIC_ACTION_KEY, receivedBundle.getSerializable(MUSIC_ACTION_KEY))
                }
                intent.putExtra(BUNDLE_KEY, newBundle)
                context?.startService(intent)
            }
        }
    }
}
