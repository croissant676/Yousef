package dev.kason.yousef.server

import kotlin.math.absoluteValue


fun interface Validator {
    fun validateCards(cards: List<Card>): Boolean

    companion object {
        val SingleCard: Validator = Validator { it.size == 1 }
        val Set: Validator = Validator { cards ->
            val value = cards.first().value
            cards.all { it.value == value }
        }

        fun Set(size: Int): Validator = Validator { cards ->
            val value = cards.first().value
            cards.size == size && cards.all { it.value == value }
        }

        fun Set(min: Int, max: Int) = Set(min..max)

        fun Set(sizeRange: IntRange = 0..Int.MAX_VALUE): Validator = Validator { cards ->
            val value = cards.first().value
            cards.size in sizeRange && cards.all { it.value == value }
        }

        val Run: Validator = Validator { cards ->
            if (cards.size < 3) return@Validator false
            if (cards.any { it.value == 0 }) return@Validator false
            val sorted = cards.sortedBy { it.value }
            val first = sorted.first().value
            val diff = sorted[1].value - first
            sorted.drop(2).all { it.value - first == diff } && diff.absoluteValue == 1
        }

        fun Run(size: Int): Validator = Validator { cards ->
            if (cards.size != size) return@Validator false
            if (cards.any { it.value == 0 }) return@Validator false
            val sorted = cards.sortedBy { it.value }
            val first = sorted.first().value
            val diff = sorted[1].value - first
            sorted.drop(2).all { it.value - first == diff } && diff.absoluteValue == 1
        }

        fun Run(min: Int, max: Int) = Run(min..max)

        fun Run(sizeRange: IntRange = 0..Int.MAX_VALUE): Validator = Validator { cards ->
            if (cards.size !in sizeRange) return@Validator false
            if (cards.any { it.value == 0 }) return@Validator false
            val sorted = cards.sortedBy { it.value }
            val first = sorted.first().value
            val diff = sorted[1].value - first
            sorted.drop(2).all { it.value - first == diff } && diff.absoluteValue == 1
        }

        val Flush = Validator { cards ->
            if (cards.size < 5) return@Validator false
            val suit = cards.first().suit
            cards.all { it.suit == suit }
        }

        fun Flush(size: Int): Validator = Validator { cards ->
            if (cards.size != size) return@Validator false
            val suit = cards.first().suit
            cards.all { it.suit == suit }
        }

        fun Flush(min: Int, max: Int) = Flush(min..max)

        fun Flush(sizeRange: IntRange): Validator = Validator { cards ->
            if (cards.size !in sizeRange) return@Validator false
            val suit = cards.first().suit
            cards.all { it.suit == suit }
        }

        val StrictSuitRun: Validator = Validator { cards ->
            cards.all { it.suit == cards.first().suit } && Run.validateCards(cards)
        }

        fun StrictSuitRun(size: Int): Validator = Validator { cards ->
            cards.all { it.suit == cards.first().suit } && Run(size).validateCards(cards)
        }

        fun StrictSuitRun(min: Int, max: Int) = StrictSuitRun(min..max)

        fun StrictSuitRun(sizeRange: IntRange): Validator = Validator { cards ->
            cards.all { it.suit == cards.first().suit } && Run(sizeRange).validateCards(cards)
        }

    }
}

