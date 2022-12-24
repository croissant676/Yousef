package dev.kason.yousef.server

import kotlinx.serialization.Serializable

@Serializable
enum class Suit {
    Clubs,
    Diamonds,
    Hearts,
    Spades;
}

@Serializable
data class Card(val suit: Suit, val value: Int) {
    init {
        // jokers have value 0, spades = black, hearts = red
        if (value == 0) {
            require(suit == Suit.Spades || suit == Suit.Hearts)
        } else {
            require(value in 1..13)
        }
    }

    val countingValue: Int
        get() = value.coerceAtMost(10)
}

fun createDeck(multiple: Int = 1): ArrayDeque<Card> {
    val deck = ArrayDeque<Card>()
    repeat(multiple) {
        for (suit in Suit.values()) {
            for (value in 1..13) {
                deck += Card(suit, value)
            }
        }
        deck += Card(Suit.Spades, 0)
        deck += Card(Suit.Hearts, 0)
    }
    deck.shuffle()
    return deck
}

fun createJokerLessDeck(multiple: Int = 1): ArrayDeque<Card> {
    val deck = ArrayDeque<Card>()
    repeat(multiple) {
        for (suit in Suit.values()) {
            for (value in 1..13) {
                deck += Card(suit, value)
            }
        }
    }
    deck.shuffle()
    return deck
}