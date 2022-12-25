package dev.kason.yousef.server

import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Player(
    var name: String, val websocket: WebSocketSession,
    override val room: Room
) : Room.Entity {

    // their join timestamp
    // if the owner leaves, the "oldest" player becomes the new owner
    val joinTimestamp = System.currentTimeMillis()

    var numberOfGamesPlayed = 0
        internal set
    var numberOfGamesLost = 0
        internal set

    val survivalPercentage: Double
        get() {
            if (numberOfGamesPlayed == 0) return 0.0
            return (numberOfGamesPlayed - numberOfGamesLost) / numberOfGamesPlayed.toDouble()
        }

    init {
        room.playerSet += this // add the player to the room
    }

    suspend fun sendMessage(message: Message) {
        val text = Json.encodeToString(message)
        websocket.send(Frame.Text(text))
    }

    suspend fun receiveRequest(): Request {
        // if not text, ignore
        val frame = websocket.incoming.receive()
        if (frame !is Frame.Text) return receiveRequest()
        val text = frame.readText()
        return Json.decodeFromString(text)
    }

    // closes the websocket
    // and removes the player from the room
    suspend fun closeWith(
        closeReason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "Client closed the connection")
    ) {
        websocket.close(closeReason)
        // remove from room
        room.removePlayer(this)
    }

}