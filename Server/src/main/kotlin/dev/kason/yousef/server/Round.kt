package dev.kason.yousef.server

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class Round(val game: Game, val roundNumber: Int) : Room.Entity by game {

    // number of turns elapsed
    private var turnsElapsed = 0

    // current turn index
    private var currentTurnIndex = 0

    // the order of the players
    // should be set in Game
    lateinit var playerOrder: List<Player>
        internal set

    // keep track of the player's hands
    internal val playerHands = mutableMapOf<Player, MutableList<Card>>()

    private val turns: MutableList<Turn> = mutableListOf()

    // access the current player
    val currentPlayer: Player
        get() = playerOrder[currentTurnIndex]

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

    suspend fun dealCards() {
        // initialize the player's hands
        playerOrder.forEach { playerHands[it] = mutableListOf() }
        // deal cards to each player
        for (player in playerOrder) {
            repeat(settings.playerHandSize) {
                player.hand += draw()
            }
        }
        // add a card to the discard pile
        discardPile += draw()
    }

    // draw a card from the deck
    suspend fun draw(): Card {
        if (deck.isEmpty()) {
            onDeckEmpty() // add the discard pile and stuff, etc
        }
        return deck.removeLast()
    }

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


    suspend fun startTurn() {
        // we send a message to the player
        // to indicate that it is their turn
        currentPlayer.sendMessage(TurnIndicatorMessage)
        // we also send an update of everything...
        updateStateFor(currentPlayer)
        // and wait for a response
        var request = repeatUntilValidRequest {
            it is DiscardRequest ||
                (turnsElapsed >= settings.minimumBeforeCall && it is CallRequest)
            // if turns elapsed is 3, it's the 4th round
            // 0 index
        }
        // by now the request should be either a discard or a call
        // if the player requests to call
        if (request is CallRequest) {
            val winningPlayers = handleCallRequest()
            // we tell the other players who beat the caller
            // we also need to reveal everyone's cards and stuff, etc.

        }
        // if not, then the request is to discard
        // check if the cards are valid
        if (!isValidDiscard((request as DiscardRequest).cards)) {
            // invalid
            // retry until player discards a valid set of cards
            request = repeatUntilValidRequest { _request ->
                _request is DiscardRequest && isValidDiscard(_request.cards)
            }
        }
        // the request contains a valid set of cards to discard
        val discardingCards = (request as DiscardRequest).cards.sortedBy { it.value }
        // remove the cards from the player's hand
        currentPlayer.hand.removeCards(discardingCards)
        // add the cards to the discard pile
        discardPile += discardingCards
        // tell player that their discard request has been processed! :)
        currentPlayer.sendMessage(ValidRequestMessage)

        // update everyone
        updateEveryone()

        // now we wait for a draw request
        val drawRequest = repeatUntilValidRequest {
            it is DrawRequest
        }

        val source = (drawRequest as DrawRequest).source
        val card: Card = if (source == DrawSource.Deck) draw() // if the player wants to draw from the deck
        else discardPile.removeLast()  // if not, then the player wants to draw from the discard

        // add the card to the player's hand
        currentPlayer.hand += card
        // update everyone
        updateEveryone()
    }

    // sends an update to everyone
    private suspend fun updateEveryone() {
        for (player in playerOrder) {
            updateStateFor(player)
        }
    }

    // sends a reveal cards message to all the players
    // which exposes their hands
    // also sends tells others which other players beat the person who called in score
    // as in, had a lower hand value.
    private suspend fun revealPlayerCards(lowerScorers: List<String>) {
        fun createRep(player: Player): RevealCardsPlayerRepresentation = RevealCardsPlayerRepresentation(
            playerName = player.name,
            cards = player.hand,
            totalValue = player.handValue
        )
        val revealCardsMessage = RevealCardsMessage(
            lowerScorers = lowerScorers,
            players = playerOrder.map(::createRep)
        )
        for (player in playerOrder) {
            player.sendMessage(revealCardsMessage)
        }
    }

    fun isValidDiscard(cards: List<Card>, player: Player = currentPlayer): Boolean {
        if (!player.hand.containsAll(cards)) return false
        return room.validators.any { it.validate(cards) }
    }

    fun handleCallRequest(): List<Player> {
        val callerSum = currentPlayer.handValue
        val otherPlayers = room.playerSet.filter { it != currentPlayer }
        // check if caller has the smallest value hand
        if (otherPlayers.all { it.handValue > callerSum }) {
            // if so, the call is successful
            // the current player does not receive points
            currentPlayer.scoreRecord.addRecord(
                // add a new record
                Game.RoundScoreRecord.Skipped(
                    player = currentPlayer,
                    roundNumber = this.roundNumber
                )
            );
            // everyone else receives the same number of points as their hand value
            otherPlayers.forEach {
                it.scoreRecord.addRecord(
                    // add a new record
                    Game.RoundScoreRecord.Regular(
                        player = it,
                        roundNumber = this.roundNumber,
                        cards = it.hand,
                    )
                )
            }
            return emptyList()
        }
        //if not, the call is a miscall
        // find the players that had a less or equal hand value
        val winningPlayers = otherPlayers.filter { it.handValue <= callerSum }
        // the current player receives the miscall punishment
        currentPlayer.scoreRecord.addRecord(
            // add a new record
            Game.RoundScoreRecord.Miscall(
                player = currentPlayer,
                roundNumber = this.roundNumber
            )
        )
        // everyone else does not receive points
        otherPlayers.forEach {
            it.scoreRecord.addRecord(
                // add a new record
                Game.RoundScoreRecord.Skipped(
                    player = it,
                    roundNumber = this.roundNumber
                )
            )
        }
        return winningPlayers
    }

    private suspend fun endRoundWithoutWinner() {
        // everyone gets a regular record added to the score
        // with the cards in their hand
        playerOrder.forEach { player ->
            val record = Game.RoundScoreRecord.Regular(
                player = player,
                roundNumber = roundNumber,
                cards = player.hand.toList()
            )
            player.scoreRecord.addRecord(record)
        }
    }

    // function that retries requests until it receives a valid request
    suspend fun repeatUntilValidRequest(
        retryMessage: Message? = null, predicate: (Request) -> Boolean
    ): Request {
        var request: Request? = null
        var changedValue: Boolean
        do {
            try {
                if (request != null) {
                    currentPlayer.sendMessage(retryMessage ?: InvalidRequestMessage)
                }
                // if an exception occurs while we try to receive request
                // and request is not initialized
                // we must check to make sure that predicate isn't run with the uninitialized
                // request as that would result in an exception
                request = currentPlayer.receiveRequest()
                changedValue = true
            } catch (e: Exception) {
                changedValue = false
            }
            // we only run the predicate on new values
        } while (changedValue && !predicate(request!!))
        return request!!
    }

    @Serializable
    data class PlayerState(
        val player: String,
        val order: Int,
        val handSize: Int
    )

    internal fun createPlayerStateLambda(playerOrderNumber: Int, player: Player): PlayerState = PlayerState(
        player = player.name,
        order = playerOrderNumber,
        handSize = player.hand.size
    )

}

// A turn played by a player
@Serializable(with = Turn.Serializer::class)
sealed class Turn(val round: Round, val turnNumber: Int) : Room.Entity by round {

    // the player that is playing this turn
    val player: Player = round.currentPlayer

    // represents a yousef call
    // no state: no properties
    class Call(round: Round, turnNumber: Int) : Turn(round, turnNumber)

    // regular turn

    class Regular(round: Round, turnNumber: Int) : Turn(round, turnNumber) {
        // the cards that were placed at the beginning
        // if empty, indicates that the player has not placed any cards
        val placedCards = mutableListOf<Card>()

        // the draw source
        lateinit var drawSource: DrawSource
            internal set

        // the card that was drawn
        var drawnCard: Card? = null
            internal set
    }

    // serializer for the turn
    object Serializer : KSerializer<Turn> {

        // we keep track of the round number, turn number, player name, and the type of turn
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Turn") {
            element<Int>("round_number")
            element<Int>("turn_number")
            element<String>("player")
            element<String>("type")
            // if it's a regular, we also need to insert placed cards, draw source, and drawn card
            // these must be nullable because they aren't present if it's a call
            element<List<Card>?>("placed_cards")
            element<DrawSource?>("draw_source")
            element<Card?>("drawn_card")
        }

        override fun deserialize(decoder: Decoder): Turn {
            throw UnsupportedOperationException() // should not be used
        }

        override fun serialize(encoder: Encoder, value: Turn) {
            val compositeEncoder = encoder.beginStructure(descriptor)
            compositeEncoder.encodeIntElement(descriptor, 0, value.round.roundNumber)
            compositeEncoder.encodeIntElement(descriptor, 1, value.turnNumber)
            compositeEncoder.encodeStringElement(descriptor, 2, value.player.name)
            val typeString = when (value) {
                is Call -> "call"
                is Regular -> "regular"
            }
            compositeEncoder.encodeStringElement(descriptor, 3, typeString)
            if (value is Regular) { // if it's a regular turn
                compositeEncoder.encodeSerializableElement(
                    descriptor,
                    4,
                    ListSerializer(Card.serializer()),
                    value.placedCards
                )
                compositeEncoder.encodeSerializableElement(
                    descriptor,
                    5,
                    DrawSource.serializer(),
                    value.drawSource
                )
                compositeEncoder.encodeSerializableElement(
                    descriptor,
                    6,
                    Card.serializer(),
                    value.drawnCard!!
                )
                // we serialize the data as well
            }
            compositeEncoder.endStructure(descriptor)
        }

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
// the player's hand
val Player.hand: MutableList<Card>
    get() {
        val game = currentGame ?: throw IllegalStateException("Game not started")
        val currentRound = game.currentRound ?: throw IllegalStateException("Round not started")
        return currentRound.playerHands[this] ?: throw IllegalStateException("Hand not initialized")
    }

val Player.handValue: Int
    get() = hand.sumValues()