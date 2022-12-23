package dev.kason.yousef.server.data

import dev.kason.yousef.server.Card
import dev.kason.yousef.server.DrawSource
import kotlinx.serialization.Serializable

@Serializable
sealed interface Request

@Serializable
object CallRequest : Request

@Serializable
data class TurnDiscardingRequest(
    val cards: List<Card>
): Request

@Serializable
data class TurnDrawingRequest(
    val source: DrawSource
): Request
