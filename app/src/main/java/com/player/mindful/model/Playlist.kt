package com.player.mindful.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long> = emptyList()
)
