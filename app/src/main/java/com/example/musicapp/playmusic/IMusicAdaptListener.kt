package com.example.musicapp.playmusic

import com.example.musicapp.musiclist.Song

interface IMusicAdaptListener {
    fun setAdapterData(songs: List<Song>)
    fun startSeekbar(songDuration: Int)
}