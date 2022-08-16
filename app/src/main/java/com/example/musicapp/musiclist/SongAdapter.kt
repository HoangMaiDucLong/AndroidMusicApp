package com.example.musicapp.musiclist

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.databinding.SongItemBinding

data class Song(val title: String?, val length: Long, val artist: String?, val uri: Uri?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readParcelable(Uri::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeLong(length)
        parcel.writeString(artist)
        parcel.writeParcelable(uri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song {
            return Song(parcel)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}

interface ISongItemClickListener {
    fun onSongItemClick(song: Song)
}

class SongAdapter(private val ctx: Context) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    private val songList: List<Song> = ArrayList()

    fun setData(list : List<Song>){
        (songList as ArrayList).apply {
            clear()
            addAll(list)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val itemBinding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(itemBinding.root, itemBinding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        songList[position].apply {
            holder.title.text = this.title
            holder.artist.text = this.artist
            holder.thumbnail.also {
                it.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, R.drawable.default_noti_background))
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.clipToOutline = true
                }
            }
            holder.menuToggle.setOnClickListener {
                onSongMenuClick(this)
            }
            holder.itemView.setOnClickListener {
                (ctx as ISongItemClickListener).onSongItemClick(this)
            }
        }
    }

    private fun onSongMenuClick(song: Song) {}

    override fun getItemCount(): Int = songList.size

    class SongViewHolder(itemView: View, binding: SongItemBinding) : RecyclerView.ViewHolder(itemView){
        val title: TextView = binding.songTitle
        val artist: TextView = binding.songArtist
        val thumbnail: ImageView = binding.songThumbnail
        val menuToggle: ImageView = binding.songMenu
    }
}
