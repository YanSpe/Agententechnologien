package de.dailab.jiacvi.aot.gridworld


/** EnvironmentAgent -> Server (ask), replied with StartGameResponse */
data class StartGameMessage(val envId: String, val antIDs: List<String>)
data class StartGameResponse(
        /** width and height of current grid */
        val size: Position,
        /** nest and therefor starting position of ants */
        val nestPosition: Position,
        /** only if activated: list of obstacle positions */
        val obstacles: List<Position>?
)

/** Server -> "broadcast", current turn */
data class GameTurnInform(val gameTurn: Int)

/** Server -> EnvironmentAgent (tell), score = percentage of total food collected */
data class EndGameMessage(val foodCollected: Int, val totalFood: Int, val score: Double)


/** AntAgent -> Server (ask), replied with AntActionResponse */
data class AntActionRequest(val antId: String, val action: AntAction)
/** flags: see Model.kt comments; state=true, flag=HAS_FOOD after move request means new position holds food source*/
data class AntActionResponse(val state: Boolean, val flag: ActionFlag = ActionFlag.NONE)

// custom messages
data class EnvironmentSetUpAntMessage(val position: Position)
data class AntTurnInformation(val turn: Int)
data class PheromoneMessage(val position: Position, var boolNestPheromone: Boolean, var amount: Double)
data class InspectPheromoneEnvironmentMessage(val position: Position, var boolNestPheromone: Boolean)
data class ReturnPheromoneEnvironmentMessage(val p0: Position, val p1: Position, val p2: Position)