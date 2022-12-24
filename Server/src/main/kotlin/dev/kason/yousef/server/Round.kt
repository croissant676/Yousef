package dev.kason.yousef.server


class Round(val game: Game, val roundNumber: Int) : Room.Entity by game {

    // number of turns elapsed
    private var turnsElapsed = 0

    // current turn index
    private var currentTurnIndex = 0

    // keep track of the player's hands
    internal val playerHands = mutableMapOf<Player, MutableList<Card>>()

    private val turns: MutableList<Turn> = mutableListOf()

}

class Turn(val round: Int) {


}



val Player.hand: List<Card>
    get() {
        val game = currentGame ?: throw IllegalStateException("Game not started")
        val currentRound = game.currentRound ?: throw IllegalStateException("Round not started")
        return currentRound.playerHands[this] ?: throw IllegalStateException("Hand not initialized")
    }

val Player.handValue: Int
    get() = hand.sumValues()