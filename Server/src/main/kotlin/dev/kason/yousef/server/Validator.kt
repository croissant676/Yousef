package dev.kason.yousef.server

// validates whether a set of cards can be placed
abstract class Validator(val name: String) {
    abstract fun validate(cards: List<Card>): Boolean

    @Suppress("FunctionName")
    companion object {
        // validator that checks whether there is 1 card
        // placing a single card is always allowed
        val LoneCard = object : Validator("Lone Card") {
            override fun validate(cards: List<Card>): Boolean = cards.size == 1
        }

        // validator that checks whether there is a pair (overshadowed by the set validator)
        // equal to Set(2)
        val Pair = object : Validator("Pair") {
            override fun validate(cards: List<Card>): Boolean = cards.size == 2 && cards[0].value == cards[1].value
        }

        // validator that checks whether the list of cards all have the same value
        fun Set(sizes: IntRange = 2..Int.MAX_VALUE) = object : Validator("Set") {
            override fun validate(cards: List<Card>): Boolean =
                cards.size in sizes && cards.all { it.value == cards[0].value }
        }

        // Run
        // returns a run validator that makes sure that the size of the run is in the given range
        // and that the delta values of the cards (the difference in card values for any 2 cards) are in the given list
        // if allowJokers is true, then jokers (value 0) can be used in the run (for ex: preceding an Ace)
        // if strictSuits is true, then all the cards must also have the same suit.
        fun Run(
            sizes: IntRange = 3..Int.MAX_VALUE,
            possibleDeltas: List<Int> = listOf(1, -1),
            allowJokers: Boolean = false,
            strictSuits: Boolean = false
        ) =
            object : Validator("Run") {
                override fun validate(cards: List<Card>): Boolean {
                    if (cards.size !in sizes) return false
                    if (!allowJokers && cards.any { it.isJoker }) return false
                    if (strictSuits && cards.map { it.suit }.distinct().size != 1) return false
                    val sorted = cards.sortedBy { it.value }
                    val firstDelta = sorted[1].value - sorted[0].value
                    if (firstDelta !in possibleDeltas) return false
                    for (index in 1 until sorted.size - 1) {
                        val delta = sorted[index + 1].value - sorted[index].value
                        if (delta != firstDelta) return false
                    }
                    return true
                }
            }

        val DefaultValidators = listOf(LoneCard, Set(), Run())

        // extra validators: not used by default.
        fun Flush(sizes: IntRange = 5..Int.MAX_VALUE) = object : Validator("Flush") {
            override fun validate(cards: List<Card>): Boolean {
                if (cards.size !in sizes) return false
                val suit = cards[0].suit
                return cards.all { it.suit == suit }
            }
        }
    }

    fun withName(name: String): Validator {
        val currentValidator = this
        return object : Validator(name) {
            override fun validate(cards: List<Card>): Boolean = currentValidator.validate(cards)
        }
    }

    override fun equals(other: Any?): Boolean = other is Validator && other.name == name
    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "Validator($name)"
}

// some helper functions

fun Validator.and(other: Validator, validatorName: String? = null): Validator =
    object : Validator(validatorName ?: "$name + ${other.name}") {
        override fun validate(cards: List<Card>): Boolean =
            this.validate(cards) && other.validate(cards)
    }

fun Validator.or(other: Validator, validatorName: String? = null): Validator =
    object : Validator(validatorName ?: "$name | ${other.name}") {
        override fun validate(cards: List<Card>): Boolean =
            this.validate(cards) || other.validate(cards)
    }

operator fun Validator.plus(other: Validator) = this.and(other)