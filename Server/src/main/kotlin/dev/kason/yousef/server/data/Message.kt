package dev.kason.yousef.server.data

import dev.kason.yousef.server.Card
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Message

@Serializable
@SerialName("current_player")
data class CurrentPlayerUpdateMessage(
    val player: String,
    val turn: Int,
    val round: Int
) : Message

@Serializable
@SerialName("discard_card")
data class DiscardCardMessage(
    val player: String,
    val cards: List<Card>,
    val discardTopCard: Card,
    val deckSize: Int,
    val discardSize: Int
) : Message

@Serializable
@SerialName("draw")
data class DrawMessage(
    val card: Card
) : Message

@Serializable
@SerialName("general_player_update")
data class GeneralPlayerUpdateMessage(
    val cards: List<Card>,
    val score: Int,
    val turn: Int,
    val round: Int,
    val discardTopCard: Card,
    val deckSize: Int,
    val discardSize: Int
) : Message

@Serializable
@SerialName("invalid_discard")
object InvalidDiscardMessage : Message