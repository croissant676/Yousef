package dev.kason.yousef.server

import kotlinx.serialization.Serializable
import java.util.*

// represents a room where players can start / play games
class Room(_customRoomCode: String? = null) {

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
        var sixNineRules: Boolean = false,
        var isPublic: Boolean = true
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

    suspend fun onGameEnd() {
        currentGame = null
    }

    // this method assumes that the player websocket has already been closed
    suspend fun removePlayer(player: Player) {
        playerSet.remove(player)
        // send a player left message to everyone
        broadcast(PlayerLeftMessage(player.name))
        if (player == owner) {
            if (playerSet.isEmpty()) {
                // remove from room manager
                RoomManager.rooms.remove(roomCode)
                return
            }
            // if the owner left, the oldest player becomes the new owner
            owner = playerSet.minByOrNull { it.joinTimestamp }!!
            //  send a room owner update message to everyone
            broadcastExcluding(RoomOwnerUpdateMessage(owner.name), owner)
            // we exclude owner bc we need to send a whole new message to the owner
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