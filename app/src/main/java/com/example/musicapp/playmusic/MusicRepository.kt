package com.example.musicapp.playmusic

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.musicapp.musiclist.Song
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future


class MusicRepository private constructor() {
    private lateinit var context: Context

    companion object {
        val songs = ArrayList<Song>()
        var storageVer: String = ""
        fun getInstance(ctx: Context) = MusicRepository().apply {
            context = ctx
        }
    }

    fun getAllSongs() : List<Song> {
        if(MediaStore.getVersion(context) != storageVer){
            updateMusicRepo()
            storageVer = MediaStore.getVersion(context)
        }
        return songs
    }

    private fun updateMusicRepo(){
        val future : Future<List<Song>> = Executors.newSingleThreadExecutor().run {
            submit(LoadMusicCallable(context))
        }
        try {
            songs.apply {
                clear()
                addAll(future.get())
            }
        } catch(ex : InterruptedException){}
        catch (ex : ExecutionException){}
    }
}

class LoadMusicCallable(private val ctx: Context) : Callable<List<Song>> {
    private val musicFolderRelativePath = "Android/media/com.Slack/%"

    private val collection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private val projection =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.RELATIVE_PATH,
            )
        else
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
            )

    private val selection =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        else null

    private val selectionArgs =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            arrayOf(musicFolderRelativePath)
        else null

    override fun call(): List<Song> {
        val res: List<Song> = arrayListOf()
        ctx.applicationContext.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)
                val duration = cursor.getLong(durationCol)
                val artist = cursor.getString(artistCol)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                (res as ArrayList).add(Song(title, duration, artist, contentUri))
            }
            cursor.close()
        }
        return res
    }
}
