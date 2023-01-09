package dev.kason.yousef.server

import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Message // messages are from the server to the client

@Serializable
@SerialName("owner_update")
data class RoomOwnerUpdateMessage(
    @SerialName("new_owner")
    val newOwner: String
) : Message


@Serializable
@SerialName("player_left")
data class PlayerLeftMessage(
    val username: String
) : Message

@Serializable
@SerialName("player_turn")
// indicates that it is the player's turn
object TurnIndicatorMessage : Message
