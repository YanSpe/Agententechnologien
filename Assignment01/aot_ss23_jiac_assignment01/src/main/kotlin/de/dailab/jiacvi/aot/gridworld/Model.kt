package de.dailab.jiacvi.aot.gridworld

import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.random.Random

enum class AntAction {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST,
    TAKE,       // take food from active food source
    DROP        // drop food at nest
}

enum class ActionFlag {
    NO_ACTIVE_GAME, // ant is not registered or no game started
    MAX_ACTIONS,    // ants can only do 1 action per turn
    OBSTACLE,       // border of grid or obstacle (#) in grid
    NO_FOOD,        // ant has no food to drop or is not at active food source to take
    NO_NEST,        // ant is not at nest while trying to drop
    HAS_FOOD,       // new position is active food source or ant has food and can't take more
    NONE
}

data class GridworldGame(
        /** size of the grid */
    val size: Position,
        /** current game turn */
    var turn: Int,
        /** maximum number of turns in this game */
    val maxTurns: Int,
        /** ant nest of this game */
    val nest: Nest,
        /** food mapped by ID */
    val foodSources: MutableMap<String, Food>,
        /** set of cells that are blocked due to obstacles  */
    val obstacles: MutableSet<Position>,
        /** relative path to log file from project root */
    var logfile: String
) {
    /** Pretty-print the game state, only for debugging... */
    fun prettyPrint(): String {
        val buffer = StringBuffer()
        // some statistics about game turns, food, etc.
        buffer.append(if (turn > maxTurns) "FINAL\n" else String.format("TURN %d/%d\n", turn, maxTurns))


        buffer.append("Food Sources\n")
        foodSources.values.sortedBy { it.created }.forEach { buffer.append("$it\n") }
        buffer.append("Nest\n$nest\n")

        // what is where?
        val elements = mutableMapOf<Position, Any>()
        foodSources.values.filter { it.available > 0 && it.created <= turn}
            .forEach { elements[it.position] = it }  // associateBy?

        nest.ants.forEach { elements[it.position] = it }
        elements[nest.position] = nest


        // print the grid
        for (y in 0 until size.y) {
            for (x in 0 until size.x) {
                val p = Position(x, y)
                val at = elements[p]
                buffer.append(when {
                    at is Food    -> "\t${at.id}"
                    at is Ant   -> "\t${at.id}"
                    at is Nest -> "\tN"
                    p in obstacles -> "\t#"
                    else           -> "\t."
                })
            }
            buffer.append("\n")
        }
        return buffer.toString()
    }

    /** Save the Pretty-printed game state, in log file */
    fun log() {
        try {
            File(this.logfile).appendText("${this.prettyPrint()}\n")
        } catch (e: IOException){
            System.err.println("Can't open logfile $e")
        }
    }



}

data class Nest (
        val position: Position,
        /** set of  Ants associated with this nest */
        val ants: MutableSet<Ant>,
        /** amount of food that was stored or dropped here */
        var food: Int
)


data class Ant(
    val id: String,
    /** current position in gridworld */
    var position: Position,
    /** turn when last AntAction was executed */
    var lastTurn: Int,
    var hasFood: Boolean
)

data class Food(
    val id: String,
    val position: Position,
    /** initial value this food source had */
    val value: Int,
    /** turn when the food was made available */
    val created: Int,
    /** amount of food not yet taken */
    var available: Int
)


data class Position(val x: Int, val y: Int) {
    companion object {
        /** get new random position within the bounds defined by the size parameter. */
        fun randomPosition(size: Position) = Position(Random.nextInt(size.x), Random.nextInt(size.y))
    }

    /**
     * Create and return new (optional) position for given move and grid size. The size
     * is optional and used for checking the maximum values, can be null if you know the
     * position will be valid (e.g. after a server confirm message)
     */
    fun applyMove(action: AntAction, size: Position? = null): Position? {
        var x2 = this.x
        var y2 = this.y
        when (action) {
            AntAction.NORTH -> y2--
            AntAction.NORTHEAST -> {y2--; x2++}
            AntAction.EAST  -> x2++
            AntAction.SOUTHEAST -> {y2++; x2++}
            AntAction.SOUTH -> y2++
            AntAction.SOUTHWEST -> {y2++; x2--}
            AntAction.WEST  -> x2--
            AntAction.NORTHWEST  -> {y2--; x2--}
            else -> {}
        }
        return if (size == null || (0 <= x2 && x2 < size.x && 0 <= y2 && y2 < size.y)) {
            Position(x2, y2)
        } else {
            null
        }
    }
}
