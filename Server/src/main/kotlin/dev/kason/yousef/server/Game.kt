package dev.kason.yousef.server

import dev.kason.yousef.server.data.CallRequest
import dev.kason.yousef.server.data.CurrentPlayerUpdateMessage
import dev.kason.yousef.server.data.GeneralPlayerUpdateMessage
import dev.kason.yousef.server.data.TurnDiscardingRequest
import kotlin.math.absoluteValue

class Game(override val room: Room) : RoomEntity {

    private val playerScores: MutableMap<Player, MutableList<Int>> = mutableMapOf()
    val Player.scores: List<Int>
        get() = playerScores[this] ?: emptyList()
    val Player.totalScore: Int
        get() = scores.sum()

}

class Round(val game: Game, val turnOrder: List<Player>) : RoomEntity {
    override val room: Room
        get() = game.room

    var turnIndex = 0
    var turnCount = 0

    val currentPlayer: Player
        get() = turnOrder[turnIndex]

    // creates deck of cards
    val deck: ArrayDeque<Card> = createDeck(room.settings.deckMultiplier)

    // create discard pile
    val discardPile = ArrayDeque<Card>()
    fun incrementPlayers() {
        if (turnIndex == turnOrder.size - 1) {
            turnIndex = 0
            turnCount++
        } else {
            turnIndex++
        }
    }

    private val playerHands: MutableMap<Player, MutableList<Card>> = mutableMapOf()
    val Player.hand: List<Card>
        get() = playerHands[this] ?: error("Player $this is not in the game")
    val Player.sumCards: Int
        get() = hand.sumOf { it.value }


    fun dealCards() {
        for (player in room.players) {
            playerHands[player] = mutableListOf()
        }
        for (player in room.players) {
            repeat(room.settings.playerHandSize) {
                playerHands[player]?.add(deck.removeFirst())
            }
        }
    }

    /**
     * @return Player who called
     * */
    suspend fun beginRound(): Player {
        dealCards()
        while (true) {
            incrementPlayers()
            val turn = Turn(this, currentPlayer)
            turn.play()
            if (turn.isCall) {
                playerCall()
            }
        }
    }

    fun playerCall() {
        currentPlayer.hand
    }
}

class Turn(val round: Round, val player: Player) : RoomEntity {
    override val room: Room
        get() = round.room

    var isCall: Boolean = false
    var placedCards: List<Card>? = null
    var cardSupplier: DrawSource? = null
    var pickedUpCard: Card? = null

    val isFinished get() = placedCards != null
    val hasPlacedCards get() = placedCards != null

    suspend fun play() {
        player.sendMessage(CurrentPlayerUpdateMessage(player.name, round.turnCount, round.turnIndex))
        var request = player.receiveRequest()
        if (request is CallRequest) {
            isCall = true
            return
        }
        if (request !is TurnDiscardingRequest) {
            error("Invalid request $request")
        }
        val cards = request.cards
        if (room.validators.none { it.validateCards(cards) }) {
            pla
            error("Invalid cards $cards")
        }
        placedCards = cards
        player.sendMessage(
            GeneralPlayerUpdateMessage(
                with(round) { player.hand },
                with(game) {

                }
            )
        )


    }
}

enum class DrawSource {
    Deck,
    DiscardPile;
}


fun interface Validator {
    fun validateCards(cards: List<Card>): Boolean

    companion object {
        val SingleCard: Validator = Validator { it.size == 1 }
        val Set: Validator = Validator { cards ->
            val value = cards.first().value
            cards.all { it.value == value }
        }
        val Run: Validator = Validator { cards ->
            if (cards.size < 3) return@Validator false
            if (cards.any { it.value == 0 }) return@Validator false
            val sorted = cards.sortedBy { it.value }
            val first = sorted.first().value
            val diff = sorted[1].value - first
            sorted.drop(2).all { it.value - first == diff } && diff.absoluteValue == 1
        }
        val SixNineValidator = Validator { cards ->
            if (cards.size != 2) return@Validator false
            val first = cards.first().value
            val second = cards.last().value
            if (first == 6 && second == 9) return@Validator true
            if (first == 9 && second == 6) return@Validator true
            return@Validator false
        }
    }
}

