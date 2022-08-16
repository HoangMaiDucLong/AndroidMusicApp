package com.example.musicapp.playmusic

import android.content.Context
import com.example.musicapp.musiclist.Song

interface MusicsContract {
    interface Presenter {
        fun getLocalSongs()
        fun launchSong(ctx : Context)
        fun playSong(ctx : Context, song: Song)
        fun pauseSong(ctx : Context)
        fun resumeSong(ctx : Context)
        fun nextSong(ctx : Context)
        fun prevSong(ctx : Context)
    }

    interface View {
        fun onFetchSongsSuccess(songs : List<Song>)
        fun startSeekbar(songDuration: Int)
    }
}