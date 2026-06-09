package com.player.mindful.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.player.mindful.model.Track

class PlayerService : Service() {

    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    private val binder = PlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val eqBands = ShortArray(5) { 0 }
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
    var onSkipNextRequested: (() -> Unit)? = null
    var onSkipPreviousRequested: (() -> Unit)? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playPause()
            ACTION_NEXT -> onSkipNextRequested?.invoke()
            ACTION_PREVIOUS -> onSkipPreviousRequested?.invoke()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlayerService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { playPause() }
                override fun onPause() { playPause() }
                override fun onSkipToNext() { onSkipNextRequested?.invoke() }
                override fun onSkipToPrevious() { onSkipPreviousRequested?.invoke() }
                override fun onStop() { stop() }
            })
            isActive = true
        }
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
        updateMediaSession()
        onStateChanged?.invoke()
    }

    fun playPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            positionMs = mp.currentPosition; mp.pause(); isPlaying = false
        } else {
            mp.seekTo(positionMs); mp.start(); isPlaying = true
        }
        updateMediaSession()
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
        updateMediaSession()
        onStateChanged?.invoke()
    }

    fun setVolume(v: Int) { currentVolume = v; applyVolume() }

    fun setBalance(l: Float, r: Float) { balanceL = l; balanceR = r; applyVolume() }

    private fun applyVolume() {
        val f = currentVolume / 10f
        mediaPlayer?.setVolume(balanceL * f, balanceR * f)
    }

    fun setEqBand(band: Int, levelMb: Short) {
        if (band !in 0..4) return
        eqBands[band] = levelMb
        try { equalizer?.setBandLevel(band.toShort(), levelMb) } catch (_: Exception) {}
    }

    fun setBassBoost(strength: Short) {
        bassBoostStr = strength
        try { if (strength > 0) { bassBoost?.enabled = true; bassBoost?.setStrength(strength) }
              else { bassBoost?.enabled = false } } catch (_: Exception) {}
    }

    fun setSpeedPitch(speed: Float, pitch: Float) {
        playbackSpeed = speed; pitchFactor = pitch
        try { mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch) } catch (_: Exception) {}
    }

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
        updateMediaSession()
        onStateChanged?.invoke(); onTrackCompleted?.invoke()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel("player", "Player", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun updateMediaSession() {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, getCurrentPositionMs().toLong(), 1f)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack?.title ?: "")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentTrack?.artist ?: "")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentTrack?.durationMs ?: 0L)
                .build()
        )
        getSystemService(NotificationManager::class.java).notify(1, buildNotification())
    }

    private fun actionPendingIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this, action.hashCode(), Intent(this, PlayerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, "player")
            .setContentTitle(currentTrack?.title ?: "Player")
            .setContentText(currentTrack?.artist ?: "")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", actionPendingIntent(ACTION_PREVIOUS))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                "Play/Pause", actionPendingIntent(ACTION_PLAY_PAUSE)
            )
            .addAction(android.R.drawable.ic_media_next, "Next", actionPendingIntent(ACTION_NEXT))
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

    override fun onDestroy() {
        mediaSession.release()
        releaseEffects(); mediaPlayer?.release(); super.onDestroy()
    }

    companion object {
        private const val ACTION_PLAY_PAUSE = "com.player.mindful.action.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.player.mindful.action.NEXT"
        private const val ACTION_PREVIOUS = "com.player.mindful.action.PREVIOUS"
    }
}
