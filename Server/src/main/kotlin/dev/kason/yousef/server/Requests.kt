package dev.kason.yousef.server

import kotlinx.serialization.Serializable

@Serializable
sealed interface Request // requests are from the client to the server

@Serializable
data class CreateRoomRequest(
    val roomCode: String? = null,
    val username: String
) : Request

