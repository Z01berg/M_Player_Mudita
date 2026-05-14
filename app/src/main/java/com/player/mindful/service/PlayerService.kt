package com.player.mindful.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Binder
import android.os.IBinder
import com.player.mindful.model.Track

class PlayerService : Service() {

    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    private val binder = PlayerBinder()
    private var mediaPlayer: MediaPlayer? = null

    // Audio effects — recreated each new track
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // Persisted effect settings
    private val eqBands = ShortArray(5) { 0 }   // millibels per band; 5 bands: sub/bass/mid/pres/treble
    private var bassBoostStr: Short = 0
    private var playbackSpeed: Float = 1.0f
    private var pitchFactor: Float = 1.0f
    private var balanceL: Float = 1.0f
    private var balanceR: Float = 1.0f

    var currentTrack: Track? = null
        private set
    var isPlaying: Boolean = false
        private set
    var positionMs: Int = 0
        private set
    var currentVolume: Int = 7
        private set

    var onStateChanged: (() -> Unit)? = null
    var onTrackCompleted: (() -> Unit)? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    fun play(track: Track) {
        releaseEffects()
        mediaPlayer?.release()
        currentTrack = track
        mediaPlayer = MediaPlayer().apply {
            setDataSource(track.uri)
            prepare()
            try { playbackParams = PlaybackParams().setSpeed(playbackSpeed).setPitch(pitchFactor) } catch (_: Exception) {}
            start()
            setOnCompletionListener { onCompletion() }
        }
        isPlaying = true
        applyVolume()
        attachEffects()
        onStateChanged?.invoke()
    }

    fun playPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            positionMs = mp.currentPosition; mp.pause(); isPlaying = false
        } else {
            mp.seekTo(positionMs); mp.start(); isPlaying = true
        }
        onStateChanged?.invoke()
    }

    fun seekToStep(step: Int, totalSteps: Int = 20) {
        val mp = mediaPlayer ?: return
        val target = ((currentTrack?.durationMs ?: 0L) * step / totalSteps).toInt()
        mp.seekTo(target); positionMs = target; onStateChanged?.invoke()
    }

    fun getCurrentPositionMs(): Int = mediaPlayer?.currentPosition ?: positionMs

    fun stop() {
        mediaPlayer?.release(); mediaPlayer = null
        releaseEffects()
        isPlaying = false; positionMs = 0; currentTrack = null
        onStateChanged?.invoke()
    }

    // ── Volume & Balance ──────────────────────────────────────────────────────
    fun setVolume(v: Int) { currentVolume = v; applyVolume() }

    fun setBalance(l: Float, r: Float) { balanceL = l; balanceR = r; applyVolume() }

    private fun applyVolume() {
        val f = currentVolume / 10f
        mediaPlayer?.setVolume(balanceL * f, balanceR * f)
    }

    // ── Equalizer ─────────────────────────────────────────────────────────────
    fun setEqBand(band: Int, levelMb: Short) {
        if (band !in 0..4) return
        eqBands[band] = levelMb
        try { equalizer?.setBandLevel(band.toShort(), levelMb) } catch (_: Exception) {}
    }

    // ── Bass Boost ────────────────────────────────────────────────────────────
    fun setBassBoost(strength: Short) {
        bassBoostStr = strength
        try { if (strength > 0) { bassBoost?.enabled = true; bassBoost?.setStrength(strength) }
              else { bassBoost?.enabled = false } } catch (_: Exception) {}
    }

    // ── Playback Speed & Pitch ────────────────────────────────────────────────
    fun setSpeedPitch(speed: Float, pitch: Float) {
        playbackSpeed = speed; pitchFactor = pitch
        try { mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch) } catch (_: Exception) {}
    }

    // ── Effects lifecycle ─────────────────────────────────────────────────────
    private fun attachEffects() {
        val id = mediaPlayer?.audioSessionId ?: return
        try {
            equalizer = Equalizer(0, id).apply {
                enabled = true
                for (i in eqBands.indices) setBandLevel(i.toShort(), eqBands[i])
            }
        } catch (_: Exception) {}
        try {
            bassBoost = BassBoost(0, id).apply {
                enabled = bassBoostStr > 0
                if (bassBoostStr > 0) setStrength(bassBoostStr)
            }
        } catch (_: Exception) {}
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}; equalizer = null
        try { bassBoost?.release() } catch (_: Exception) {}; bassBoost = null
    }

    private fun onCompletion() {
        isPlaying = false; positionMs = 0
        onStateChanged?.invoke(); onTrackCompleted?.invoke()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel("player", "Player", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, "player")
            .setContentTitle(currentTrack?.title ?: "Player")
            .setContentText(currentTrack?.artist ?: "")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

    override fun onDestroy() {
        releaseEffects(); mediaPlayer?.release(); super.onDestroy()
    }
}
