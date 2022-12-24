package dev.kason.yousef.server

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Player(
    var name: String, val websocket: WebSocketSession,
    override val room: Room
) : Room.Entity {


}