package dev.kason.yousef.server

import dev.kason.yousef.server.data.*
import kotlin.math.absoluteValue

// represents a game
class Game(override val room: Room) : RoomEntity {

    // the scores of the players, stored in a map
    val playerScores: MutableMap<Player, MutableList<Int>> = mutableMapOf()

    //
    val Player.scores: MutableList<Int>
        get() = playerScores[this] ?: error("Player $name is not in the game")

    //
    val Player.totalScore: Int
        get() = scores.sum()

    suspend fun end(loser: Player) {
//      TODO laugh at them
        room.game = null

        // we send a game end message to everyone
        val scores = room.players.map { createPlayerScoreRepresentation(it) }
        val gameEndMessage = GameEndMessage(loser.name, scores)
        room.players.forEach { it.sendMessage(gameEndMessage) }
        room.spectators.forEach { it.sendMessage(gameEndMessage) }

    }

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
    val Player.hand: MutableList<Card>
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
            turn.playTurn()
            if (turn.isCall) {
                playerCall()
                return currentPlayer
            }
            // otherwise, return the largest value one
            val maxScore = potentialLosers.values.maxOf { it.totalScore }
            var losers = potentialLosers.filter { it.value.totalScore == maxScore }
            if (losers.size == 1) {
                return losers.keys.first()
            }
            // if multiple are tied for the largest value, return the player with the higher sum of card values of
            // last round
            // if tied, continue to previous rounds until start
            for (round in rounds.reversed()) {
                val maxValue = losers.values.maxOf { it[round.roundNumber].score }
                losers = losers.filter { it.value[round.roundNumber].score == maxValue }
                if (losers.size == 1) {
                    return losers.keys.first()
                }
            }
            // if still tied, then return random player :shrug:
            return losers.keys.random()
        }
    }

    suspend fun play() {
        game.end(beginRound())
    }

    suspend fun playerCall() = with(game) {
        val value = currentPlayer.sumCards
        if (room.players.filter { it != currentPlayer }.any { it.sumCards <= value }) {
            // player called but someone else has a lower score
            // player loses
            room.players.forEach {
                it.scores.add(if (it != currentPlayer) 0 else room.settings.miscallPunishment)
            }
            val otherPlayer = // select another player with an equal or less score
                room.players.filter { it != currentPlayer }.minByOrNull { it.sumCards }!!
            room.players.forEach {
                it.sendMessage(
                    IncorrectCallMessage(
                        currentPlayer.name, currentPlayer.hand, currentPlayer.sumCards,
                        otherPlayer.name, otherPlayer.hand, otherPlayer.sumCards
                    )
                )
            }
        } else {
            room.players.forEach {
                it.scores.add(if (it == currentPlayer) 0 else it.sumCards)
            }
            room.players.forEach {
                it.sendMessage(
                    CorrectCallMessage(
                        currentPlayer.name, currentPlayer.hand, currentPlayer.sumCards
                    )
                )
            }
        }
        val message =
            RoundCardRevealMessage(
                room.players.map { player -> createPlayerRevealCardRepresentation(player) },
                room.players.map { player -> createPlayerScoreRepresentation(player) }
            )
        // send everyone a message with the scores
        room.players.forEach {
            it.sendMessage(message)
        }
    }

    fun createGeneralPlayerUpdateMessage(player: Player): GeneralPlayerUpdateMessage = with(game) {
        return GeneralPlayerUpdateMessage(
            cards = player.hand,
            score = player.totalScore,
            turn = turnCount,
            round = turnCount,
            discardTopCard = discardPile.last(),
            deckSize = deck.size,
            discardSize = discardPile.size
        )
    }

    fun createDiscardCardMessage(player: Player, cards: List<Card>): DiscardCardMessage = with(game) {
        return DiscardCardMessage(
            player = player.name,
            cards = cards,
            discardTopCard = discardPile.last(),
            deckSize = deck.size,
            discardSize = discardPile.size
        )
    }

    fun createDrawUpdateMessage(): DrawUpdateMessage = with(game) {
        return DrawUpdateMessage(
            deckSize = deck.size,
            discardSize = discardPile.size,
            discardTopCard = discardPile.last()
        )
    }
}

class Turn(val round: Round, val player: Player) : RoomEntity {
    override val room: Room
        get() = round.room

    var isCall: Boolean = false
    var placedCards: List<Card>? = null
    var drawingSource: DrawSource? = null
    var pickedUpCard: Card? = null
    var attemptCount: Int = 0

    val isFinished get() = placedCards != null
    val hasPlacedCards get() = placedCards != null

    suspend fun playTurn() {
        while (true) {
            try {
                val success = attemptTurn()
                if (success) break
            } catch (e: Exception) {
                attemptCount++
            }
        }
    }

    // Attempt to play a turn
    // Returns true if the turn was successful
    // Returns false if the turn was unsuccessful
    suspend fun attemptTurn(): Boolean {
        player.sendMessage(CurrentPlayerUpdateMessage(player.name, round.turnCount, round.turnIndex))
        var request = player.receiveRequest()
        if (request is CallRequest) {
            if (round.turnCount < room.settings.minimumBeforeCall) {
                player.sendMessage(InvalidCallMessage)
                return false
            }
            isCall = true
            return true
        }
        if (request !is TurnDiscardingRequest) {
            return false
        }
        val cards = request.cards
        if (cards.isEmpty()) {
            player.sendMessage(InvalidDiscardMessage)
            return false
        }
        with(round) {
            // make sure that the players have the cards they are trying to discard
            if (!player.hand.containsAll(cards)) {
                player.sendMessage(InvalidDiscardMessage)
                return false
            }
        }
        if (room.validators.none { it.validateCards(cards) }) {
            player.sendMessage(InvalidDiscardMessage)
            return false
        }
        placedCards = cards
        // remove cards from player's hand
        with(round) {
            player.hand.removeAll(cards)
        }
        round.discardPile.addAll(cards)
        player.sendMessage(round.createGeneralPlayerUpdateMessage(player))
        // send a basic update to all players
        for (otherPlayer in room.players) {
            if (otherPlayer == player) continue
            otherPlayer.sendMessage(round.createDiscardCardMessage(player, cards))
        }
        // wait for them to send a draw request
        request = player.receiveRequest()
        if (request !is TurnDrawingRequest) {
            return false
        }
        this.drawingSource = request.source
        when (request.source) {
            DrawSource.Deck -> {
                val card = round.deck.removeFirst()
                if (round.deck.isEmpty()) {
                    if (round.discardPile.isEmpty()) {
                        error("Deck and discard pile are empty")
                    }
                    // remove everything but the top card from the discard pile
                    val keepCard = round.discardPile.removeLast()
                    val otherCards = round.discardPile.dropLast(1)
                    round.deck.addAll(otherCards.shuffled())
                    round.discardPile.clear()
                    round.discardPile.add(keepCard)
                }
                pickedUpCard = card
            }
            DrawSource.DiscardPile -> {
                if (round.discardPile.isEmpty()) {
                    error("Discard pile is empty")
                }
                val card = round.discardPile.removeLast()
                pickedUpCard = card
            }
        }
        return true
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

