package com.example.musicapp.playmusic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.musicapp.Constants.BUNDLE_KEY
import com.example.musicapp.Constants.IS_PLAYING_KEY
import com.example.musicapp.Constants.MUSIC_ACTION_KEY
import com.example.musicapp.Constants.SEEKBAR_PROGRESS_BUNDLE_KEY
import com.example.musicapp.Constants.SONG_BUNDLE_KEY
import com.example.musicapp.MainActivity
import com.example.musicapp.PlayMusicApp.Companion.channelId
import com.example.musicapp.R
import com.example.musicapp.musiclist.Song
import java.io.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import androidx.media.app.NotificationCompat as MediaNotificationCompat

const val NO_DELAY = 0L
const val SECOND_INTERVAL = 1000L

class PlayMusicService : Service() {
    private var player: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private val songs = ArrayList<Song>()
    private var song : Song? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var updateSeekbarExecutor = Executors.newScheduledThreadPool(1)
    private var future : ScheduledFuture<*>? = null

    companion object {
        const val notificationId : Int = 1
        const val mediaSessionName: String = "_PlayMusicService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, mediaSessionName)
        notificationBuilder = NotificationCompat.Builder(this, channelId).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_music_note)
            setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 3)
                    .setMediaSession(mediaSession?.sessionToken)
            )
            setContentIntent(
                Intent(applicationContext, MainActivity::class.java).let { intent ->
                    PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            })
        }

        songs.apply {
            clear()
            addAll(MusicRepository.getInstance(baseContext).getAllSongs())
        }

        player = MediaPlayer().apply {
            setOnCompletionListener {
                stopSeekbarProgress()
                nextMusic()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getBundleExtra(BUNDLE_KEY)?.apply {
            song = getParcelable(SONG_BUNDLE_KEY) ?: song
            handleActionEvent(this)
        }
        return START_NOT_STICKY
    }

    private fun handleActionEvent(bundle: Bundle?){
        when(bundle?.getSerializable(MUSIC_ACTION_KEY) as MusicAction){
            MusicAction.LAUNCH -> {
                launchMusic()
            }
            MusicAction.PLAY -> {
                song = bundle.getParcelable(SONG_BUNDLE_KEY) ?: song
                playMusic()
            }
            MusicAction.NEXT -> nextMusic()
            MusicAction.PREVIOUS -> prevMusic()
            MusicAction.PAUSE -> pauseMusic()
            MusicAction.RESUME -> resumeMusic()
            MusicAction.CLEAR -> stopSelf()
            MusicAction.SEEK -> {
                seekTo(bundle.getInt(SEEKBAR_PROGRESS_BUNDLE_KEY))
            }
            else -> {}
        }
    }

    private fun launchMusic(){
        if(song == null) {
            song = songs[songs.indices.random()]
            setSongInfoToNotification()
            setSongToPlayer()
            sendNotification()
            notifyMainWithAction(MusicAction.LAUNCH)
        } else {
            Intent(MUSIC_ACTION_KEY).also { intent ->
                intent.putExtra(MUSIC_ACTION_KEY, MusicAction.RESTORE_PROGRESS)
                intent.putExtra(SONG_BUNDLE_KEY, song)
                intent.putExtra(IS_PLAYING_KEY, player?.isPlaying)
                intent.putExtra(SEEKBAR_PROGRESS_BUNDLE_KEY, player?.currentPosition)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }
    }

    private fun playMusic() {
        setSongToPlayer()
        setSongInfoToNotification()
        player?.start()
        sendNotification()
        notifyMainWithAction(MusicAction.PLAY)
        startSeekbarProgress()
    }

    private fun nextMusic() {
        song = songs[(songs.indexOf(song)+1) % songs.size]
        playMusic()
        notifyMainWithAction(MusicAction.NEXT)
    }

    private fun prevMusic() {
        song = songs[(songs.indexOf(song)-1).mod(songs.size)]
        playMusic()
        notifyMainWithAction(MusicAction.PREVIOUS)
    }

    private fun pauseMusic() {
        if(player?.isPlaying != true) return
        player?.pause()
        sendNotification()
        notifyMainWithAction(MusicAction.PAUSE)
    }

    private fun resumeMusic() {
        if(player?.isPlaying == true) return
        player?.start()
        sendNotification()
        notifyMainWithAction(MusicAction.RESUME)
    }

    private fun seekTo(progress: Int) {
        player?.seekTo(progress)
    }

    private fun setSongToPlayer(){
        player?.apply {
            song?.uri?.let {
                reset()
                player?.setDataSource(applicationContext, it)
                prepareAsync()
            }
        }
    }

    private fun stopSeekbarProgress(){
        future?.cancel(true)
        updateSeekbarExecutor.awaitTermination(10L, TimeUnit.MILLISECONDS)
    }

    private fun startSeekbarProgress() {
        future = updateSeekbarExecutor.scheduleAtFixedRate(
            {
                Intent(MUSIC_ACTION_KEY).also { intent ->
                    intent.putExtra(MUSIC_ACTION_KEY, MusicAction.UPDATE_PROGRESS)
                    intent.putExtra(SEEKBAR_PROGRESS_BUNDLE_KEY, player?.currentPosition ?: 0)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            },
            NO_DELAY,
            SECOND_INTERVAL,
            TimeUnit.MILLISECONDS
        )
    }

    private fun setSongInfoToNotification(){
        notificationBuilder?.apply {
            setLargeIcon(BitmapFactory.decodeResource(
                resources,
                R.drawable.default_noti_background
            ))
            setContentTitle(song?.title)
            setContentText(song?.artist)
            setSubText(song?.title)
        }
    }

    private fun notifyMainWithAction(action: MusicAction){
        Intent(MUSIC_ACTION_KEY).also { intent ->
            intent.putExtra(MUSIC_ACTION_KEY, action)
            intent.putExtra(SONG_BUNDLE_KEY, song)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntent(ctx: Context, action: MusicAction) : PendingIntent =
        Intent(ctx.applicationContext, MusicReceiver::class.java).run {
            val bundle = Bundle().apply {
                putSerializable(MUSIC_ACTION_KEY, action)
            }
            putExtra(BUNDLE_KEY, bundle)
            setAction(action.toString())
            PendingIntent.getBroadcast(ctx.applicationContext, 0, this, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun sendNotification() {
        notificationBuilder?.apply {
            clearActions()
            if(player?.isPlaying == true){
                addAction(R.drawable.ic_prev, "Prev",
                    getPendingIntent(baseContext, MusicAction.PREVIOUS)
                )
                addAction(R.drawable.ic_pause, "Pause",
                    getPendingIntent(baseContext, MusicAction.PAUSE)
                )
                addAction(R.drawable.ic_next, "Next",
                    getPendingIntent(baseContext, MusicAction.NEXT)
                )
                addAction(R.drawable.ic_clear, "Cancel",
                    getPendingIntent(baseContext, MusicAction.CLEAR)
                )
            } else {
                addAction(R.drawable.ic_prev, "Prev",
                    getPendingIntent(baseContext, MusicAction.PREVIOUS)
                )
                addAction(R.drawable.ic_play, "Resume",
                    getPendingIntent(baseContext, MusicAction.RESUME)
                )
                addAction(R.drawable.ic_next, "Next",
                    getPendingIntent(baseContext, MusicAction.NEXT)
                )
                addAction(R.drawable.ic_clear, "Cancel",
                    getPendingIntent(baseContext, MusicAction.CLEAR)
                )
            }
        }?.build().also {
            startForeground(notificationId, it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.apply {
            stop()
            release()
        }
        player = null
        mediaSession?.release()
        mediaSession = null
        song = null
    }
}

enum class MusicAction : Serializable {
    LAUNCH,
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS,
    RESUME,
    CLEAR,
    SEEK,
    UPDATE_PROGRESS,
    RESTORE_PROGRESS
}
