package com.example.musicapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.Constants.MUSIC_ACTION_KEY
import com.example.musicapp.Constants.SEEKBAR_PROGRESS_BUNDLE_KEY
import com.example.musicapp.Constants.SONG_BUNDLE_KEY
import com.example.musicapp.musiclist.ISongItemClickListener
import com.example.musicapp.musiclist.Song
import com.example.musicapp.musiclist.SongAdapter
import com.example.musicapp.playmusic.IMusicAdaptListener
import com.example.musicapp.playmusic.MusicAction
import com.example.musicapp.playmusic.MusicPresenter
import com.example.musicapp.utils.Utils

class MainActivity : AppCompatActivity(), ISongItemClickListener, IMusicAdaptListener {
    lateinit var songAdapter : SongAdapter
    lateinit var musicPresenter: MusicPresenter
    var isPlaying : Boolean = false
    private val receiver =  ServiceToMainReceiver()
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: ImageView
    private lateinit var currentPosTextView: TextView
    private lateinit var durationTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekBar = findViewById(R.id.sb_music)
        playPauseButton = findViewById(R.id.player_play_pause)
        currentPosTextView = findViewById(R.id.curr_pos_player)
        durationTextView = findViewById(R.id.player_duration)

        setupSongList()
        setupActionBroadcastReceiver()

        musicPresenter.launch()
    }

    private fun setupSongList(){
        songAdapter = SongAdapter(this)

        musicPresenter = MusicPresenter(this)
        musicPresenter.requestAllSongs()

        findViewById<RecyclerView>(R.id.song_list).apply {
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
        musicPresenter.playSong(song)
    }

    private fun startBottomPlayer(song: Song) {
        setSongInfoToPlayer(song)

        findViewById<ImageView>(R.id.player_prev).setOnClickListener {
            musicPresenter.prevSong()
        }

        playPauseButton.apply {
            setOnClickListener {
                if(isPlaying){
                    musicPresenter.pauseSong()
                } else {
                    musicPresenter.resumeSong()
                }
            }
            setImageDrawable(
                AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_play_white)
            )
        }

        findViewById<ImageView>(R.id.player_next).setOnClickListener {
            musicPresenter.nextSong()
        }

        findViewById<ImageView>(R.id.player_img).apply{
            setImageDrawable(
                AppCompatResources.getDrawable(this@MainActivity, R.drawable.default_noti_background)
            )
            clipToOutline = true
        }

        findViewById<ConstraintLayout>(R.id.bottom_player_holder).apply {
            elevation = 10F
        }

        findViewById<SeekBar>(R.id.sb_music).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) musicPresenter.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setSongInfoToPlayer(song: Song){
        findViewById<TextView>(R.id.player_title).text = song.title
        findViewById<TextView>(R.id.player_artist).text = song.artist
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun playPrevNextSong(song: Song) {
        isPlaying = true
        setSongInfoToPlayer(song)
        playPauseButton.setImageDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_pause_white)
        )
        startSeekbar(song.length.toInt())
    }

    fun resumeSong() {
        isPlaying = true
        playPauseButton.setImageDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_pause_white)
        )
    }

    fun pauseSong(){
        isPlaying = false
        playPauseButton.setImageDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_play_white)
        )
    }

    override fun setAdapterData(songs: List<Song>) {
        songAdapter.setData(songs)
    }

    inner class ServiceToMainReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            val song = intent?.getParcelableExtra<Song>(SONG_BUNDLE_KEY)
            intent?.getSerializableExtra(MUSIC_ACTION_KEY)?.also { action ->
                when(action){
                    MusicAction.LAUNCH -> song?.let { startBottomPlayer(it) }
                    MusicAction.PLAY -> isPlaying = true
                    MusicAction.NEXT, MusicAction.PREVIOUS -> song?.let { playPrevNextSong(it) }
                    MusicAction.PAUSE -> pauseSong()
                    MusicAction.RESUME -> resumeSong()
                    MusicAction.UPDATE_PROGRESS -> {
                        updateSeekbarProgress(intent.getIntExtra(SEEKBAR_PROGRESS_BUNDLE_KEY, 0))
                    }
                    else -> pauseSong()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun startSeekbar(songDuration: Int){
        durationTextView.text = Utils.durationToString(songDuration)
        seekBar.max = songDuration
        seekBar.min = 0
    }

    private fun updateSeekbarProgress(intExtra: Int) {
        currentPosTextView.text = Utils.durationToString(intExtra)
        seekBar.progress = intExtra
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}
