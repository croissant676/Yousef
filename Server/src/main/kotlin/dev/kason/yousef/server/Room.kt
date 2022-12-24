package dev.kason.yousef.server

// represents a room where players can start / play games
class Room(customRoomCode: String? = null) {

    val room: Room
}

class Room(val roomCode: String = generateRandomRoomCode()) {
    val participants = sortedSetOf(compareBy(Participant::name))
    val players get() = participants.filterIsInstance<Player>()
    val spectators get() = participants.filterIsInstance<Spectator>()

    var game: Game? = null

    data class Settings(
        var playerCap: Int = Int.MAX_VALUE,
        var allowChat: Boolean = true,
        var strictSuits: Boolean = false,
        var allowJokers: Boolean = true,
        var scoreLimit: Int = 100,
        var deckMultiplier: Int = 1,
        var playerHandSize: Int = 4,
        var endWhenDeckEmpty: Boolean = true,
        var minimumBeforeCall: Int = 3,
        var turnLimit: Int = Int.MAX_VALUE,
        var miscallPunishment: Int = 30,
        var informalMoves: Boolean = true,
        var passcode: String? = null,
        var sixNineRules: Boolean = false
    )

    val settings = Settings()
    val validators = mutableListOf(
        Validator.SingleCard,
        Validator.Set,
        Validator.Run
    )

    val settings = Settings()

    // the validators for this room
    // validators make sure that the cards that are played are valid
    // these can be changed through plugins or settings
    val validators =
        Validator.DefaultValidators.toMutableList()

    fun saveSettings() {
        if (settings.sixNineRules) {
            validators.add(Validator.SixNineValidator)
        } else {
            validators.remove(Validator.SixNineValidator)
        }
    }

    // entity tied to a room
    interface Entity {

        val room: Room

        val settings: Settings
            get() = room.settings

        val currentGame: Game?
            get() = room.currentGame

        val players: List<Player>
            get() = room.players

    }
}