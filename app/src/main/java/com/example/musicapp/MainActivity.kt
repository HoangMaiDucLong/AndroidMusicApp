package com.example.musicapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.Constants.IS_PLAYING_KEY
import com.example.musicapp.Constants.MUSIC_ACTION_KEY
import com.example.musicapp.Constants.SEEKBAR_PROGRESS_BUNDLE_KEY
import com.example.musicapp.Constants.SONG_BUNDLE_KEY
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.musiclist.ISongItemClickListener
import com.example.musicapp.musiclist.Song
import com.example.musicapp.musiclist.SongAdapter
import com.example.musicapp.playmusic.MusicAction
import com.example.musicapp.playmusic.MusicPresenter
import com.example.musicapp.playmusic.MusicRepository
import com.example.musicapp.playmusic.MusicsContract
import com.example.musicapp.utils.Utils

class MainActivity : AppCompatActivity(),
    ISongItemClickListener,
    MusicsContract.View {

    private lateinit var binding : ActivityMainBinding
    private lateinit var songAdapter : SongAdapter
    lateinit var musicPresenter: MusicPresenter
    var isPlaying : Boolean = false
    private val receiver =  ServiceToMainReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSongList()
        setupActionBroadcastReceiver()

        musicPresenter.launchSong(applicationContext)
    }

    private fun setupSongList(){
        songAdapter = SongAdapter(this)

        musicPresenter = MusicPresenter(MusicRepository.getInstance(applicationContext)).also {
            it.setView(this)
        }
        musicPresenter.getLocalSongs()

        binding.songList.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun setupActionBroadcastReceiver(){
        val filter = IntentFilter().apply {
            addAction(MUSIC_ACTION_KEY)
            addCategory(MUSIC_ACTION_KEY)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    override fun onSongItemClick(song: Song) {
        startBottomPlayer(song)
        setPauseButton()
        musicPresenter.playSong(this, song)
    }

    private fun setPlayButton(){
        binding.playerPlayPause.setImageDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_play_white)
        )
    }

    private fun setPauseButton(){
        binding.playerPlayPause.setImageDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_pause_white)
        )
    }

    override fun onFetchSongsSuccess(songs: List<Song>) {
        songAdapter.setData(songs)
    }

    override fun startSeekbar(songDuration: Int){
        binding.apply {
            playerDuration.text = Utils.durationToString(songDuration)
            sbMusic.max = songDuration
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                sbMusic.min = 0
            }
        }
    }

    private fun restoreSeekbar(songDuration: Int, currPos: Int){
        binding.apply {
            playerDuration.text = Utils.durationToString(songDuration)
            sbMusic.max = songDuration
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                sbMusic.min = 0
            }
            sbMusic.progress = currPos
        }
    }

    private fun setSongInfoToPlayer(song: Song){
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist
    }

    inner class ServiceToMainReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val song = intent?.getParcelableExtra<Song>(SONG_BUNDLE_KEY)
            intent?.getSerializableExtra(MUSIC_ACTION_KEY)?.also { action ->
                when(action){
                    MusicAction.LAUNCH -> song?.let {
                        startBottomPlayer(it)
                        setPlayButton()
                    }
                    MusicAction.PLAY -> isPlaying = true
                    MusicAction.NEXT, MusicAction.PREVIOUS -> song?.let { playPrevNextSong(it) }
                    MusicAction.PAUSE -> pauseSong()
                    MusicAction.RESUME -> resumeSong()
                    MusicAction.UPDATE_PROGRESS -> {
                        updateSeekbarProgress(intent.getIntExtra(SEEKBAR_PROGRESS_BUNDLE_KEY, 0))
                    }
                    MusicAction.RESTORE_PROGRESS -> {
                        song?.let {
                            restoreMusicProgress(
                                song,
                                intent.getIntExtra(SEEKBAR_PROGRESS_BUNDLE_KEY, 0),
                                intent.getBooleanExtra(IS_PLAYING_KEY, false)
                            )
                        }
                    }
                    else -> pauseSong()
                }
            }
        }
    }

    private fun startBottomPlayer(song: Song) {
        setSongInfoToPlayer(song)

        binding.apply {
            playerPlayPause.apply {
                setOnClickListener {
                    if (isPlaying) {
                        musicPresenter.pauseSong(applicationContext)
                    } else {
                        musicPresenter.resumeSong(applicationContext)
                    }
                }
//                setPlayButton()
            }

            playerPrev.setOnClickListener {
                musicPresenter.prevSong(applicationContext)
            }

            playerNext.setOnClickListener {
                musicPresenter.nextSong(applicationContext)
            }

            playerImg.apply {
                setImageDrawable(
                    AppCompatResources.getDrawable(
                        this@MainActivity,
                        R.drawable.default_noti_background
                    )
                )
                clipToOutline = true
            }

            bottomPlayerHolder.apply {
                elevation = 10F
            }

            sbMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) musicPresenter.seekTo(applicationContext, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    fun playPrevNextSong(song: Song) {
        isPlaying = true
        setSongInfoToPlayer(song)
        setPauseButton()
        startSeekbar(song.length.toInt())
    }

    fun pauseSong(){
        isPlaying = false
        setPlayButton()
    }

    fun resumeSong() {
        isPlaying = true
        setPauseButton()
    }

    private fun updateSeekbarProgress(intExtra: Int) {
        binding.apply {
            currPosPlayer.text = Utils.durationToString(intExtra)
            sbMusic.progress = intExtra
        }
    }

    private fun restoreMusicProgress(song: Song, currPos: Int, isPlayerPlaying: Boolean) {
        isPlaying = isPlayerPlaying
        startBottomPlayer(song)
        if(isPlaying) {
            setPauseButton()
        } else {
            setPlayButton()
        }
        restoreSeekbar(song.length.toInt(), currPos)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}
