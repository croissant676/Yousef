package dev.kason.yousef.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Request // requests are from the client to the server

@Serializable
data class CreateRoomRequest(
    val roomCode: String? = null,
    val username: String
) : Request

@Serializable
@SerialName("call")
object CallRequest : Request

// cards that are discarded
@Serializable
@SerialName("discard")
data class DiscardRequest(
    val cards: List<Card>
) : Request

// source that player want to draw from
@Serializable
@SerialName("draw")
data class DrawRequest(
    val source: DrawSource
) : Request
