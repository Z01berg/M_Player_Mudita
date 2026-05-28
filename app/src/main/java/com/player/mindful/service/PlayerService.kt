package com.player.mindful.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.player.mindful.model.Track

class PlayerService : Service() {

    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    private val binder = PlayerBinder()
    private var mediaPlayer: MediaPlayer? = null

    var currentTrack: Track? = null
        private set
    var isPlaying: Boolean = false
        private set
    var positionMs: Int = 0
        private set

    var onStateChanged: (() -> Unit)? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    fun play(track: Track) {
        mediaPlayer?.release()
        currentTrack = track
        mediaPlayer = MediaPlayer().apply {
            setDataSource(track.uri)
            prepare()
            start()
            setOnCompletionListener { onCompletion() }
        }
        isPlaying = true
        onStateChanged?.invoke()
    }

    fun playPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            positionMs = mp.currentPosition
            mp.pause()
            isPlaying = false
        } else {
            mp.seekTo(positionMs)
            mp.start()
            isPlaying = true
        }
        onStateChanged?.invoke()
    }

    fun seekToStep(step: Int, totalSteps: Int = 20) {
        val mp = mediaPlayer ?: return
        val target = ((currentTrack?.durationMs ?: 0L) * step / totalSteps).toInt()
        mp.seekTo(target)
        positionMs = target
        onStateChanged?.invoke()
    }

    fun getCurrentPositionMs(): Int = mediaPlayer?.currentPosition ?: positionMs

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        positionMs = 0
        currentTrack = null
        onStateChanged?.invoke()
    }

    private fun onCompletion() {
        isPlaying = false
        positionMs = 0
        onStateChanged?.invoke()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("player", "Player", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, "player")
            .setContentTitle(currentTrack?.title ?: "Player")
            .setContentText(currentTrack?.artist ?: "")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
