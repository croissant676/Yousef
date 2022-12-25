package dev.kason.yousef.server

import kotlinx.serialization.Serializable

object Representations {

    @Serializable
    data class Room (
        val players: List<Player>,
    )

    @Serializable
    data class Player (
        val name: String,
        val joinTimestamp: Long
    )

}