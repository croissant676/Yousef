package dev.kason.yousef.server

import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
    // null values are not encoded
}


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
        val text = json.encodeToString(message)
        websocket.send(Frame.Text(text))
    }

    suspend fun receiveRequest(): Request {
        // if not text, ignore
        val frame = websocket.incoming.receive()
        if (frame !is Frame.Text) return receiveRequest()
        val text = frame.readText()
        return json.decodeFromString(text)
    }

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Request> receiveRequestOfType(): T? = repeatUntilValidRequest {
        it is T
    } as T?

    // function that retries requests until it receives a valid request
    suspend fun repeatUntilValidRequest(
        retryMessage: Message? = null, predicate: (Request) -> Boolean
    ): Request {
        var request: Request? = null
        var changedValue: Boolean
        do {
            try {
                if (request != null) {
                    sendMessage(retryMessage ?: InvalidRequestMessage)
                }
                // if an exception occurs while we try to receive request
                // and request is not initialized
                // we must check to make sure that predicate isn't run with the uninitialized
                // request as that would result in an exception
                request = receiveRequest()
                changedValue = true
            } catch (e: Exception) {
                changedValue = false
            }
            // we only run the predicate on new values
        } while (changedValue && !predicate(request!!))
        return request!!
    }

    // closes the websocket
    // and removes the player from the room
    suspend fun closeWith(
        closeReason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "Client closed the connection")
    ) {
        websocket.close(closeReason)
        // remove from room
        room.removeAndBroadcast(this)
    }

}