package com.player.mindful.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.os.IBinder
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.player.mindful.model.PlayerTheme
import com.player.mindful.model.Track
import com.player.mindful.service.PlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("player_prefs")
private val THEME_KEY = stringPreferencesKey("theme")

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionStep = MutableStateFlow(0)
    val positionStep: StateFlow<Int> = _positionStep.asStateFlow()

    private val _volume = MutableStateFlow(7)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _theme = MutableStateFlow(PlayerTheme.MARSHALL)
    val theme: StateFlow<PlayerTheme> = _theme.asStateFlow()

    private var service: PlayerService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as PlayerService.PlayerBinder).getService()
            service?.onStateChanged = { syncFromService() }
            loadTracks()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    init {
        val ctx = getApplication<Application>()
        ctx.bindService(
            Intent(ctx, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        viewModelScope.launch {
            ctx.dataStore.data.map { it[THEME_KEY] }.collect { name ->
                if (name != null) _theme.value = PlayerTheme.valueOf(name)
            }
        }
    }

    fun cycleTheme() {
        val next = when (_theme.value) {
            PlayerTheme.MARSHALL -> PlayerTheme.SYNTH
            PlayerTheme.SYNTH -> PlayerTheme.SOVIET
            PlayerTheme.SOVIET -> PlayerTheme.MARSHALL
        }
        _theme.value = next
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[THEME_KEY] = next.name }
        }
    }

    fun playTrack(track: Track) {
        service?.play(track)
    }

    fun playPause() {
        service?.playPause()
    }

    fun seekToStep(step: Int) {
        service?.seekToStep(step)
        _positionStep.value = step
    }

    fun setVolume(v: Int) {
        _volume.value = v
        val f = v / 10f
        service?.let {
            // MediaPlayer volume is set via the service indirectly — store and apply on next play
        }
    }

    fun stop() {
        service?.stop()
    }

    private fun syncFromService() {
        val s = service ?: return
        _currentTrack.value = s.currentTrack
        _isPlaying.value = s.isPlaying
        val dur = s.currentTrack?.durationMs?.toInt() ?: 1
        val pos = s.getCurrentPositionMs()
        _positionStep.value = ((pos.toLong() * 20 / dur.toLong()).toInt()).coerceIn(0, 20)
    }

    private fun loadTracks() {
        val ctx = getApplication<Application>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val cursor: Cursor? = ctx.contentResolver.query(
            uri, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )
        val list = mutableListOf<Track>()
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                list.add(
                    Track(
                        id = it.getLong(idCol),
                        title = it.getString(titleCol) ?: "Unknown",
                        artist = it.getString(artistCol) ?: "Unknown",
                        album = it.getString(albumCol) ?: "Unknown",
                        durationMs = it.getLong(durCol),
                        uri = it.getString(dataCol) ?: ""
                    )
                )
            }
        }
        _tracks.value = list
    }

    override fun onCleared() {
        getApplication<Application>().unbindService(connection)
        super.onCleared()
    }
}
