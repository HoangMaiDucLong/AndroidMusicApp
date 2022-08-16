package com.example.musicapp.playmusic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.musicapp.Constants.BUNDLE_KEY
import com.example.musicapp.Constants.MUSIC_ACTION_KEY
import com.example.musicapp.Constants.SEEKBAR_PROGRESS_BUNDLE_KEY
import com.example.musicapp.Constants.SONG_BUNDLE_KEY
import com.example.musicapp.musiclist.Song

class MusicPresenter(private val dataSource: MusicRepository) : MusicsContract.Presenter {
    private val songs = ArrayList<Song>()
    private var mView : MusicsContract.View? = null

//    fun requestAllSongs(ctx : Context) {
//        updateSongs(ctx)
//        mView?.onFetchSongsSuccess(songs)
//    }
//
//    private fun updateSongs(ctx : Context) {
//        songs.apply {
//            clear()
//            addAll(MusicRepository.getInstance(ctx).getAllSongs())
//        }
//    }

    fun setView(view : MusicsContract.View){
        mView = view
    }

    override fun launchSong(ctx: Context){
        getLocalSongs()
//        songs[songs.indices.random()].also { launchSong ->
//            sendActionToService(ctx, MusicAction.LAUNCH, launchSong)
//        }
        sendActionToService(ctx, MusicAction.LAUNCH)
    }

    override fun playSong(ctx: Context, song: Song){
        sendActionToService(ctx, MusicAction.PLAY, song)
        mView?.startSeekbar(song.length.toInt())
    }

    override fun resumeSong(ctx : Context){
        sendActionToService(ctx, MusicAction.RESUME)
    }

    override fun pauseSong(ctx: Context){
        sendActionToService(ctx, MusicAction.PAUSE)
    }

    override fun nextSong(ctx: Context){
        sendActionToService(ctx, MusicAction.NEXT)
    }

    override fun prevSong(ctx: Context){
        sendActionToService(ctx, MusicAction.PREVIOUS)
    }

    override fun getLocalSongs() {
//        songs.apply {
//            clear()
//            addAll()
//        }
        mView?.onFetchSongsSuccess(dataSource.getAllSongs())
    }

    /**
     * Seek to a time position in the song
     * @param progress offset from the beginning of the song, in milliseconds
     */
    fun seekTo(ctx: Context, progress: Int){
        Intent(ctx, PlayMusicService::class.java).also { intent ->
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
    private fun sendActionToService(ctx: Context, action: MusicAction, song: Song? = null){
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
