package com.example.musicapp.playmusic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.ShareCompat
import com.example.musicapp.Constants.BUNDLE_KEY
import com.example.musicapp.Constants.MUSIC_ACTION_KEY
import com.example.musicapp.Constants.SEEKBAR_PROGRESS_BUNDLE_KEY
import com.example.musicapp.Constants.SONG_BUNDLE_KEY
import com.example.musicapp.DetailActivity
import com.example.musicapp.musiclist.Song

class MusicPresenter(private val ctx: Context) {
    private val songs = ArrayList<Song>()

    /**
     * Update cached song list and populate the main activity adapter with the list
     */
    fun requestAllSongs() {
        updateSongs()
        (ctx as IMusicAdaptListener).setAdapterData(songs)
    }

    /**
     * Update cached song list
     */
    private fun updateSongs() {
        songs.apply {
            clear()
            addAll(MusicRepository.getInstance(ctx).getAllSongs())
        }
    }

    /**
     * Populate song list's data
     * and start PlayMusicService with a random song
     */
    fun launch(){
        requestAllSongs()
        songs[songs.indices.random()].also { launchSong ->
            sendActionToService(MusicAction.LAUNCH, launchSong)
        }
        sendActionToService(MusicAction.LAUNCH)
    }

    /**
     * Start playing a song from the beginning
     * @param song the song to play
     */
    fun playSong(song: Song){
        sendActionToService(MusicAction.PLAY, song)
        (ctx as IMusicAdaptListener).startSeekbar(song.length.toInt())
    }

    /**
     * Resume playing the currently paused song
     */
    fun resumeSong(){
        sendActionToService(MusicAction.RESUME)
    }

    /**
     * Pause the currently playing song
     */
    fun pauseSong(){
        sendActionToService(MusicAction.PAUSE)
    }

    /**
     * Play the next song in the list
     */
    fun nextSong(){
        sendActionToService(MusicAction.NEXT)
    }

    /**
     * Play the previous song in the list
     */
    fun prevSong(){
        sendActionToService(MusicAction.PREVIOUS)
    }

    /**
     * Seek to a time position in the song
     * @param progress offset from the beginning of the song, in milliseconds
     */
    fun seekTo(progress: Int){
        Intent(ctx.applicationContext, PlayMusicService::class.java).also { intent ->
            val bundle = Bundle().also {
                it.putInt(SEEKBAR_PROGRESS_BUNDLE_KEY, progress)
                it.putSerializable(MUSIC_ACTION_KEY, MusicAction.SEEK)
            }
            intent.putExtra(BUNDLE_KEY, bundle)
            ctx.startService(intent)
        }
    }

    /**
     * Send an intent with specified action to PlayMusicService
     * @param action the action to notify service
     * @param song the song to be sent with intent, default value is null if action doesn't need
     * to be sent with a song
     */
    private fun sendActionToService(action: MusicAction, song: Song? = null){
        Intent(ctx.applicationContext, PlayMusicService::class.java).also { intent ->
            val bundle = Bundle().also {
                it.putParcelable(SONG_BUNDLE_KEY, song)
                it.putSerializable(MUSIC_ACTION_KEY, action)
            }
            intent.putExtra(BUNDLE_KEY, bundle)
            ctx.startService(intent)
        }
    }
}
