package dev.kason.yousef.server

interface RoomEntity {

    val room: Room
}

class Room(val roomCode: String = generateRandomRoomCode()) {
    val participants = sortedSetOf(compareBy(Participant::name))
    val players get() = participants.filterIsInstance<Player>()
    val spectators get() = participants.filterIsInstance<Spectator>()

    var game: Game? = null
        private set

    data class Settings(
        var playerCap: Int = Int.MAX_VALUE,
        var spectatorCap: Int = Int.MAX_VALUE,
        var allowSpectators: Boolean = true,
        var allowChat: Boolean = true,
        var filterChat: Boolean = true,
        var strictSuits: Boolean = false,
        var allowJokers: Boolean = true,
        var scoreCutting: Boolean = true,
        var scoreLimit: Int = 100,
        var deckMultiplier: Int = 1,
        var playerHandSize: Int = 4,
        var endWhenDeckEmpty: Boolean = true,
        var continueOnTie: Boolean = true,
        var minimumBeforeCall: Int = 3,
        var turnLimit: Int = Int.MAX_VALUE,
        var miscallPunishment: Int = 30,
        var informalMoves: Boolean = false,
        var passcode: String? = null,
        var sixNineRules: Boolean = false
    )

    val settings = Settings()
    val validators = mutableListOf(
        Validator.SingleCard,
        Validator.Set,
        Validator.Run
    )

    fun saveSettings() {
        if (settings.sixNineRules) {
            validators.add(Validator.SixNineValidator)
        } else {
            validators.remove(Validator.SixNineValidator)
        }
    }
}

fun generateRandomRoomCode(): String {
    // 4 random uppercase letters
    val builder = StringBuilder()
    repeat(4) {
        builder.append(('A'..'Z').random())
    }
    return builder.toString()
}