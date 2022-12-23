package dev.kason.yousef.server

import dev.kason.yousef.server.data.Message
import dev.kason.yousef.server.data.Request
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class Participant(val name: String, val websocket: WebSocketSession, override val room: Room) : RoomEntity {

    suspend fun sendMessage(message: Message) {
        val json = Json.encodeToString(message)
        websocket.send(json)
    }

    suspend fun receiveRequest(): Request {
        val frame = websocket.incoming.receive()
        if (frame !is Frame.Text) error("Received non-text frame")
        val json = frame.readText()
        return Json.decodeFromString(Request.serializer(), json)
    }
}

class Player(name: String, websocket: WebSocketSession, room: Room) : Participant(name, websocket, room) {

}

class Spectator(name: String, websocket: WebSocketSession, room: Room) : Participant(name, websocket, room) {

}