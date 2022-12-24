package dev.kason.yousef.server.data

import dev.kason.yousef.server.Card
import dev.kason.yousef.server.Game
import dev.kason.yousef.server.Player
import dev.kason.yousef.server.Round
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
@SerialName("draw_update")
data class DrawUpdateMessage(
    // update the size of the deck / discard
    val deckSize: Int,
    val discardSize: Int,
    // update the top card of the discard
    val discardTopCard: Card
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

// premature calling
@Serializable
@SerialName("invalid_call")
object InvalidCallMessage : Message

@Serializable
@SerialName("incorrect_call")
data class IncorrectCallMessage(
    // info about who called
    // and also who had the lowest score
    val caller: String,
    val callerCards: List<Card>,
    val callerScore: Int,
    val lowest: String,
    val lowestCards: List<Card>,
    val lowestScore: Int
) : Message

@Serializable
@SerialName("correct_call")
data class CorrectCallMessage(
    val caller: String,
    val callerCards: List<Card>,
    val callerScore: Int
) : Message

// reveal everyone's cards
@Serializable
data class PlayerRevealCardRepresentation(
    val player: String,
    val cards: List<Card>,
    val score: Int
)

fun Round.createPlayerRevealCardRepresentation(player: Player): PlayerRevealCardRepresentation = with(game) {
    return PlayerRevealCardRepresentation(
        player = player.name,
        cards = player.hand,
        score = player.sumCards
    )
}

@Serializable
data class PlayerScoreRepresentation(
    val player: String,
    val scores: List<Int>,
    val totalScore: Int
)

fun Game.createPlayerScoreRepresentation(player: Player): PlayerScoreRepresentation {
    return PlayerScoreRepresentation(
        player.name,
        player.scores,
        player.totalScore
    )
}

@Serializable
@SerialName("card_reveal")
data class RoundCardRevealMessage(
    val players: List<PlayerRevealCardRepresentation>,
    val scores: List<PlayerScoreRepresentation>
) : Message

// game end message
@Serializable
@SerialName("game_end")
data class GameEndMessage(
    val loser: String,
    val scores: List<PlayerScoreRepresentation>,
    val players: List<PlayerRevealCardRepresentation>
) : Message

