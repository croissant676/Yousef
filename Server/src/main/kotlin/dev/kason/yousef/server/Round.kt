package dev.kason.yousef.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Round(val game: Game, val roundNumber: Int) : Room.Entity by game {

    // number of turns elapsed
    private var turnsElapsed = 0

    // current turn index
    private var currentTurnIndex = 0

    // keep track of the player's hands
    internal val playerHands = mutableMapOf<Player, MutableList<Card>>()

    private val turns: MutableList<Turn> = mutableListOf()

    // access the current player
    val currentPlayer: Player
        get() = players[currentTurnIndex]

    val currentTurn = turns.lastOrNull()

    // the deck of cards
    val deck: ArrayDeque<Card> = createDeck(
        multiplier = settings.deckMultiplier,
        hasJokers = settings.allowJokers
    )

    val discardPile: MutableList<Card> = mutableListOf()

    suspend fun start() {
        dealCards()
    }

    fun dealCards() {
        // initialize the player's hands
        players.forEach { playerHands[it] = mutableListOf() }
        // deal cards to each player
        for (player in players) {
            repeat(settings.playerHandSize) {
                player.hand += draw()
            }
        }
        // add a card to the discard pile
        discardPile += draw()
    }

    // draw a card from the deck
    fun draw(): Card = deck.removeLast()

    suspend fun onDeckEmpty() {
        if (settings.endWhenDeckEmpty) {
            endRoundWithoutWinner()
        } else {
            val topCard = discardPile.removeLast()
            val movedCards = discardPile.dropLast(1)
            discardPile.clear()
            discardPile += topCard
            deck += movedCards
            deck.shuffle()
        }
    }

    private suspend fun endRoundWithoutWinner() {

    }

    suspend fun startTurn() {

    }

}

// A turn played by a player
// logic for this contained in the round class
// used for storing state
sealed class Turn(val round: Round, val turnNumber: Int) : Room.Entity by round {

    // the player that is playing this turn
    val player: Player = round.currentPlayer

    // represents a yousef call
    // no state: no properties
    class Call(round: Round, turnNumber: Int) : Turn(round, turnNumber)

    // regular turn
    // player places cards -> validated ->
    // if the cards are valid, the player can choose where to draw from
    // draw -> turn ends
    class Regular(round: Round, turnNumber: Int) : Turn(round, turnNumber) {
        // the cards that were placed at the beginning
        // if empty, indicates that the player has not placed any cards
        val placedCards = mutableListOf<Card>()
        var drawSource: DrawSource? = null
            internal set
        var cardDrawn: Card? = null
            internal set

        // the number of request attempts that this player has done
        // not really useful now but possibly useful in the future
        var attemptNumber: Int = 0
            internal set

        val hasPlacedCards: Boolean
            get() = placedCards.isNotEmpty()

        val isFinished: Boolean
            get() = drawSource != null
    }

}

// Represents where players can draw their cards
// Deck or Discard
@Serializable
enum class DrawSource {
    @SerialName("deck")
    Deck,

    @SerialName("discard")
    Discard
}

// api: don't modify the list
val Player.hand: MutableList<Card>
    get() {
        val game = currentGame ?: throw IllegalStateException("Game not started")
        val currentRound = game.currentRound ?: throw IllegalStateException("Round not started")
        return currentRound.playerHands[this] ?: throw IllegalStateException("Hand not initialized")
    }

val Player.handValue: Int
    get() = hand.sumValues()