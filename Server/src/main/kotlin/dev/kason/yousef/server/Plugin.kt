package dev.kason.yousef.server

class Plugin(
    val name: String,
    val version: String,
    val description: String,
) {
    init {
        require(name.isNotBlank())
        require(version.isNotBlank())
        require(description.isNotBlank())
        println("Plugin of $name with version $version loaded.")
    }

    var onInitialization: suspend Room.() -> Unit = {}
    var onGameStart: suspend Game.() -> Unit = {}
    var onGameEnd: suspend Game.() -> Unit = {}
    var onRoundStart: suspend Round.() -> Unit = {}
    var onRoundEnd: suspend Round.() -> Unit = {}
    var onTurnStart: suspend Turn.() -> Unit = {}
    var onTurnEnd: suspend Turn.() -> Unit = {}

    val validators = mutableListOf<Validator>()
    fun validate(validator: Validator) {
        validators += validator
    }
}

fun Plugin(
    name: String,
    version: String,
    description: String,
    init: Plugin.() -> Unit
): Plugin {
    val plugin = Plugin(name, version, description)
    plugin.init()
    return plugin
}

val sixNinePlugin = Plugin(
    "SixNinePlugin",
    "1.0",
    "Allows for 6-9 discards and resets a player's score to 0 when they reach 69 points"
) {
    onRoundEnd = {
        with(game) {
            room.players.filter { it.totalScore == 69 }.forEach {
                it.scores.removeLast()
                it.scores.add(-it.totalScore)
            }
        }
    }
    validate { cards ->
        if (cards.size != 2) return@validate false
        val first = cards.first().value
        val second = cards.last().value
        if (first == 6 && second == 9) return@validate true
        if (first == 9 && second == 6) return@validate true
        return@validate false
    }
}