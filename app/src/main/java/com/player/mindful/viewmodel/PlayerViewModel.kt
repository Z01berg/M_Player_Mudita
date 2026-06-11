package com.player.mindful.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.player.mindful.model.PlayerTheme
import com.player.mindful.model.PlayerUiState
import com.player.mindful.model.Playlist
import com.player.mindful.model.RepeatMode
import com.player.mindful.model.SortMode
import com.player.mindful.model.Track
import com.player.mindful.service.PlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("player_prefs")
private val THEME_KEY = stringPreferencesKey("theme")
private val PLAYLISTS_KEY = stringPreferencesKey("playlists")

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var service: PlayerService? = null
    private var sleepTimerJob: Job? = null

    private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = loadTracks()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as PlayerService.PlayerBinder).getService()
            service?.onStateChanged = { syncFromService() }
            service?.onTrackCompleted = { viewModelScope.launch { nextTrack() } }
            service?.onSkipNextRequested = { nextTrack() }
            service?.onSkipPreviousRequested = { previousTrack() }
            service?.setVolume(_state.value.volume)
            applyAllToService()
            loadTracks()
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null }
    }

    init {
        val ctx = getApplication<Application>()
        ctx.bindService(Intent(ctx, PlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
        ctx.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
        viewModelScope.launch {
            ctx.dataStore.data.map { it[THEME_KEY] }.collect { name ->
                if (name != null) _state.update { it.copy(theme = PlayerTheme.valueOf(name)) }
            }
        }
        viewModelScope.launch {
            ctx.dataStore.data.map { it[PLAYLISTS_KEY] }.collect { json ->
                val list = if (json == null) emptyList() else runCatching { Json.decodeFromString<List<Playlist>>(json) }.getOrDefault(emptyList())
                _state.update { it.copy(playlists = list) }
            }
        }
        viewModelScope.launch { while (true) { delay(500); if (_state.value.isPlaying) syncFromService() } }
    }

    fun playTrack(track: Track)  { service?.play(track) }
    fun playPause()               { service?.playPause() }
    fun stop()                    { service?.stop() }

    fun seekToStep(step: Int) {
        service?.seekToStep(step)
        _state.update { it.copy(positionStep = step) }
    }

    fun previousTrack() {
        val s = _state.value
        val list = s.tracks; if (list.isEmpty()) return
        if (s.shuffle) { previousShuffled(s, list); return }
        val idx = list.indexOfFirst { it.id == s.currentTrack?.id }
        val prev = when {
            s.repeatMode == RepeatMode.ONE && idx >= 0 -> list[idx]
            idx > 0 -> list[idx - 1]
            s.repeatMode == RepeatMode.ALL -> list.lastOrNull()
            else -> null
        }
        prev?.let { service?.play(it) }
    }

    fun nextTrack() {
        val s = _state.value
        if (s.queue.isNotEmpty()) {
            val next = s.queue.first()
            _state.update { it.copy(queue = it.queue.drop(1)) }
            service?.play(next)
            return
        }
        val list = s.tracks; if (list.isEmpty()) return
        if (s.shuffle) { nextShuffled(s, list); return }
        val idx = list.indexOfFirst { it.id == s.currentTrack?.id }
        val next = when {
            s.repeatMode == RepeatMode.ONE && idx >= 0 -> list[idx]
            idx >= 0 && idx < list.size - 1 -> list[idx + 1]
            s.repeatMode == RepeatMode.ALL -> list.firstOrNull()
            else -> null
        }
        next?.let { service?.play(it) }
    }

    private fun nextShuffled(s: PlayerUiState, list: List<Track>) {
        if (s.repeatMode == RepeatMode.ONE) { s.currentTrack?.let { service?.play(it) }; return }
        var order = s.shuffleOrder
        if (order.isEmpty() || order.size != list.size) order = list.map { it.id }.shuffled()
        var idx = s.shuffleIndex + 1
        if (idx >= order.size) {
            if (s.repeatMode != RepeatMode.ALL) return
            order = list.map { it.id }.shuffled(); idx = 0
        }
        val track = list.firstOrNull { it.id == order[idx] } ?: return
        _state.update { it.copy(shuffleOrder = order, shuffleIndex = idx) }
        service?.play(track)
    }

    private fun previousShuffled(s: PlayerUiState, list: List<Track>) {
        if (s.repeatMode == RepeatMode.ONE) { s.currentTrack?.let { service?.play(it) }; return }
        var order = s.shuffleOrder
        if (order.isEmpty() || order.size != list.size) order = list.map { it.id }.shuffled()
        var idx = s.shuffleIndex - 1
        if (idx < 0) {
            if (s.repeatMode != RepeatMode.ALL) return
            idx = order.size - 1
        }
        val track = list.firstOrNull { it.id == order[idx] } ?: return
        _state.update { it.copy(shuffleOrder = order, shuffleIndex = idx) }
        service?.play(track)
    }

    fun setVolume(v: Int)  { _state.update { it.copy(volume = v) };      service?.setVolume(v) }

    fun setBalance(idx: Int) {
        _state.update { it.copy(balanceIdx = idx) }
        val (l, r) = when (idx) { 0 -> 1.0f to 0.2f; 2 -> 0.2f to 1.0f; else -> 1.0f to 1.0f }
        service?.setBalance(l, r)
    }

    fun setEqBass(v: Int) {
        _state.update { it.copy(eqBass = v) }
        service?.setEqBand(0, toMb(v)); service?.setEqBand(1, (toMb(v) * 0.6f).toInt().toShort())
    }
    fun setEqMid(v: Int)      { _state.update { it.copy(eqMid = v) };      service?.setEqBand(2, toMb(v)) }
    fun setEqPresence(v: Int) { _state.update { it.copy(eqPresence = v) }; service?.setEqBand(3, toMb(v)) }
    fun setEqTreble(v: Int)   { _state.update { it.copy(eqTreble = v) };   service?.setEqBand(4, toMb(v)) }

    fun applyEqPreset(preset: Int) = when (preset) {
        0 -> { setEqBass(5); setEqMid(5); setEqPresence(5); setEqTreble(5) }
        1 -> { setEqBass(8); setEqMid(4); setEqPresence(6); setEqTreble(7) }
        2 -> { setEqBass(6); setEqMid(3); setEqPresence(9); setEqTreble(8) }
        3 -> { setEqBass(8); setEqMid(6); setEqPresence(5); setEqTreble(5) }
        4 -> { setEqBass(5); setEqMid(5); setEqPresence(7); setEqTreble(9) }
        else -> Unit
    }

    fun toggleEqBass(on: Boolean)     = setEqBass    (if (on) 8 else 5)
    fun toggleEqMid(on: Boolean)      = setEqMid     (if (on) 8 else 5)
    fun toggleEqPresence(on: Boolean) = setEqPresence(if (on) 8 else 5)
    fun toggleEqTreble(on: Boolean)   = setEqTreble  (if (on) 8 else 5)

    fun setBassBoost(on: Boolean) {
        _state.update { it.copy(bassBoostOn = on) }
        service?.setBassBoost(if (on) 500 else 0)
    }

    fun setSpeedIdx(idx: Int) {
        _state.update { it.copy(speedIdx = idx) }
        service?.setSpeedPitch(speedForIdx(idx), pitchForIdx(_state.value.pitchIdx))
    }

    fun setPitchIdx(idx: Int) {
        _state.update { it.copy(pitchIdx = idx) }
        service?.setSpeedPitch(speedForIdx(_state.value.speedIdx), pitchForIdx(idx))
    }

    fun setSearch(q: String) {
        _state.update { it.copy(searchQuery = q) }
        recomputeFiltered()
    }

    fun cycleSort() {
        val next = when (_state.value.sortMode) {
            SortMode.TITLE_ASC -> SortMode.ARTIST
            SortMode.ARTIST    -> SortMode.DURATION
            SortMode.DURATION  -> SortMode.TITLE_DESC
            SortMode.TITLE_DESC -> SortMode.TITLE_ASC
        }
        _state.update { it.copy(sortMode = next) }
        recomputeFiltered()
    }

    fun toggleFavorite(id: Long) {
        _state.update { it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id) }
    }

    fun toggleExpanded() { _state.update { it.copy(isExpanded = !it.isExpanded) } }

    fun toggleShuffle() {
        val s = _state.value
        if (s.shuffle) { _state.update { it.copy(shuffle = false) }; return }
        val order = s.tracks.map { it.id }.shuffled()
        val startIdx = order.indexOf(s.currentTrack?.id).let { if (it >= 0) it else 0 }
        _state.update { it.copy(shuffle = true, shuffleOrder = order, shuffleIndex = startIdx) }
    }

    fun playNext(track: Track)      { _state.update { it.copy(queue = listOf(track) + it.queue) } }
    fun addToQueueEnd(track: Track) { _state.update { it.copy(queue = it.queue + track) } }
    fun removeFromQueue(track: Track) { _state.update { it.copy(queue = it.queue.filterNot { t -> t.id == track.id }) } }
    fun reorderQueue(from: Int, to: Int) {
        _state.update {
            val q = it.queue.toMutableList()
            if (from in q.indices && to in q.indices) { val item = q.removeAt(from); q.add(to, item) }
            it.copy(queue = q)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) { _state.update { it.copy(sleepTimerEndAt = null, sleepTimerRemainingSec = null, sleepTimerTotalSec = null) }; return }
        val totalSec = minutes * 60
        val startAt = System.currentTimeMillis()
        val endAt = startAt + totalSec * 1000L
        _state.update { it.copy(sleepTimerEndAt = endAt, sleepTimerTotalSec = totalSec, sleepTimerRemainingSec = totalSec) }
        sleepTimerJob = viewModelScope.launch {
            val segments = 5
            for (step in 1..segments) {
                val checkpointAt = startAt + totalSec * 1000L * step / segments
                val waitMs = checkpointAt - System.currentTimeMillis()
                if (waitMs > 0) delay(waitMs)
                val remaining = (totalSec - totalSec * step / segments)
                _state.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            stop()
            _state.update { it.copy(sleepTimerEndAt = null, sleepTimerRemainingSec = null, sleepTimerTotalSec = null) }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val updated = _state.value.playlists + Playlist(id = System.currentTimeMillis(), name = name.trim())
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun createPlaylistWithTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        val updated = _state.value.playlists + Playlist(id = System.currentTimeMillis(), name = name.trim(), trackIds = listOf(trackId))
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun deletePlaylist(id: Long) {
        val updated = _state.value.playlists.filterNot { it.id == id }
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun renamePlaylist(id: Long, name: String) {
        if (name.isBlank()) return
        val updated = _state.value.playlists.map { if (it.id == id) it.copy(name = name.trim()) else it }
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        val updated = _state.value.playlists.map {
            if (it.id == playlistId && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
        }
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        val updated = _state.value.playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
        }
        _state.update { it.copy(playlists = updated) }
        persistPlaylists(updated)
    }

    fun playPlaylist(playlistId: Long) {
        val s = _state.value
        val playlist = s.playlists.firstOrNull { it.id == playlistId } ?: return
        val tracks = playlist.trackIds.mapNotNull { id -> s.tracks.firstOrNull { it.id == id } }
        if (tracks.isEmpty()) return
        _state.update { it.copy(queue = tracks.drop(1)) }
        service?.play(tracks.first())
    }

    private fun persistPlaylists(playlists: List<Playlist>) {
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[PLAYLISTS_KEY] = Json.encodeToString(playlists) } }
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = next) }
    }

    fun cycleTheme() {
        val next = when (_state.value.theme) {
            PlayerTheme.MARSHALL -> PlayerTheme.SYNTH
            PlayerTheme.SYNTH    -> PlayerTheme.SOVIET
            PlayerTheme.SOVIET   -> PlayerTheme.MARSHALL
        }
        _state.update { it.copy(theme = next) }
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[THEME_KEY] = next.name } }
    }

    private fun toMb(v: Int): Short = ((v - 5) * 200).toShort()
    private fun speedForIdx(i: Int) = when (i) { 0 -> 0.8f; 2 -> 1.2f; else -> 1.0f }
    private fun pitchForIdx(i: Int) = when (i) { 0 -> 0.9439f; 2 -> 1.0595f; else -> 1.0f }

    private fun recomputeFiltered() {
        val s = _state.value
        val f = if (s.searchQuery.isBlank()) s.tracks
                else s.tracks.filter { it.title.contains(s.searchQuery, ignoreCase = true) || it.artist.contains(s.searchQuery, ignoreCase = true) }
        val sorted = when (s.sortMode) {
            SortMode.TITLE_ASC  -> f.sortedBy { it.title.lowercase() }
            SortMode.TITLE_DESC -> f.sortedByDescending { it.title.lowercase() }
            SortMode.ARTIST     -> f.sortedBy { it.artist.lowercase() }
            SortMode.DURATION   -> f.sortedByDescending { it.durationMs }
        }
        val albums  = s.tracks.groupBy { it.album  }.entries.sortedBy { it.key }.map { it.key to it.value }
        val artists = s.tracks.groupBy { it.artist }.entries.sortedBy { it.key }.map { it.key to it.value }
        _state.update { it.copy(filteredTracks = sorted, albumGroups = albums, artistGroups = artists) }
    }

    private fun syncFromService() {
        val sv = service ?: return
        val dur = sv.currentTrack?.durationMs?.toInt() ?: 1
        val pos = sv.getCurrentPositionMs()
        _state.update { it.copy(
            currentTrack  = sv.currentTrack,
            isPlaying     = sv.isPlaying,
            positionStep  = ((pos.toLong() * 20 / dur.toLong()).toInt()).coerceIn(0, 20)
        ) }
    }

    private fun applyAllToService() {
        val s  = _state.value
        val sv = service ?: return
        sv.setEqBand(0, toMb(s.eqBass)); sv.setEqBand(1, (toMb(s.eqBass) * 0.6f).toInt().toShort())
        sv.setEqBand(2, toMb(s.eqMid)); sv.setEqBand(3, toMb(s.eqPresence)); sv.setEqBand(4, toMb(s.eqTreble))
        sv.setBassBoost(if (s.bassBoostOn) 500 else 0)
        sv.setSpeedPitch(speedForIdx(s.speedIdx), pitchForIdx(s.pitchIdx))
        val (l, r) = when (s.balanceIdx) { 0 -> 1.0f to 0.2f; 2 -> 0.2f to 1.0f; else -> 1.0f to 1.0f }
        sv.setBalance(l, r)
    }

    fun loadTracks() {
        val ctx = getApplication<Application>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA
        )
        val cursor: Cursor? = ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} ASC"
        )
        val list = mutableListOf<Track>()
        cursor?.use {
            val idC  = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val datC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) list.add(Track(
                it.getLong(idC), it.getString(titC) ?: "Unknown",
                it.getString(artC) ?: "Unknown", it.getString(albC) ?: "Unknown",
                it.getLong(durC), it.getString(datC) ?: ""
            ))
        }
        _state.update {
            val order = if (it.shuffle && it.shuffleOrder.size != list.size) list.map { t -> t.id }.shuffled() else it.shuffleOrder
            it.copy(tracks = list, shuffleOrder = order)
        }
        recomputeFiltered()
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        getApplication<Application>().unbindService(connection)
        super.onCleared()
    }
}
