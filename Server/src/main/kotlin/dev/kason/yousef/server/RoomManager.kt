package dev.kason.yousef.server

import io.ktor.websocket.*

object RoomManager {

    internal val rooms = mutableMapOf<String, Room>()
    val roomListing: List<Room>
        get() = rooms.values.toList().filter { it.settings.isPublic }

    val takenRoomCodes: Set<String> get() = rooms.keys

    suspend fun createRoom(createRoomRequest: CreateRoomRequest, ownerSession: WebSocketSession): Room {
        val newRoom = Room(createRoomRequest.roomCode)
        rooms[newRoom.roomCode] = newRoom
        val owner = Player(createRoomRequest.username, ownerSession, newRoom)
        newRoom.owner = owner
        return newRoom
    }

}