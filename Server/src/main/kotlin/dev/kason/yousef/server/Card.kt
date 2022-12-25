package dev.kason.yousef.server

@Serializable
enum class Suit {
    @SerialName("clubs")
    Clubs,

    @SerialName("diamonds")
    Diamonds,

    @SerialName("hearts")
    Hearts,

    @SerialName("spades")
    Spades
}

@JvmInline
@Serializable
value class Card(private val data: Byte) {
    // value determines suit / rank
    // 0 -> red joker
    // 1 -> black joker
    // 2-14 -> Ace-King of clubs
    // 15-27 -> Ace-King of diamonds
    // 28-40 -> Ace-King of hearts
    // 41-53 -> Ace-King of spades

    init {
        require(data in 0..53) { "Invalid card value: $data" }
    }

    // this constructor does not make sure that the rank is valid
    // if your rank is negative or greater than 13, you may or may not get an exception
    // and your suit may not be correct
    constructor(suit: Suit, rank: Int) : this((suit.ordinal * 13 + rank + 1).toByte())

    // suit
    // returns null if the card is a joker
    val suit: Suit?
        get() = when (data) {
            in 2..14 -> Suit.Clubs
            in 15..27 -> Suit.Diamonds
            in 28..40 -> Suit.Hearts
            in 41..53 -> Suit.Spades
            else -> null
        }

    val rank: Int
        get() = when (data) {
            in 2..14 -> data - 1
            in 15..27 -> data - 14
            in 28..40 -> data - 27
            in 41..53 -> data - 40
            else -> 0
        }

    // value is rank but face cards are 10
    val value: Int
        get() = rank.coerceAtMost(10)

    val isJoker: Boolean
        get() = (data == 0.toByte()) || (data == 1.toByte())

    override fun toString(): String {
        // [Rank] of [Suit]
        // Jokers are just "Red Joker" or "Black Joker"
        return when (data) {
            0.toByte() -> "Red Joker"
            1.toByte() -> "Black Joker"
            else -> {
                val rankString = when (rank) {
                    1 -> "Ace"
                    11 -> "Jack"
                    12 -> "Queen"
                    13 -> "King"
                    else -> rank.toString()
                }
                "$rankString of $suit"
            }
        }
    }

}

fun List<Card>.sumValues(): Int = sumOf { it.value }

fun createDeck(multiplier: Int = 1, hasJokers: Boolean = true): ArrayDeque<Card> {
    val deck = ArrayDeque<Card>()
    repeat(multiplier) {
        if (hasJokers) {
            deck.add(Card(0))
            deck.add(Card(1))
        }
        for (suit in Suit.values()) {
            for (rank in 1..13) {
                deck.add(Card(suit, rank))
            }
        }
    }
    deck.shuffle()
    return deck
}

