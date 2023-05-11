package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String) : Agent(overrideName = envId) {
    // TODO you might need to put some variables to save stuff here

    private val numberOfAnts: Int = 1
    private val antAgentsId: ArrayList<String> = ArrayList()

    private lateinit var size: Position
    private lateinit var nestPosition: Position
    private var obstacles: ArrayList<Position>? = null

    //private val xNest: IntArray = intArrayOf(size.x)
    //private val yNest: IntArray = intArrayOf(size.y)
    //private val nestPheromones: Array<IntArray> = arrayOf(xNest, yNest)
    var nestPheromones: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }

    //private val xFood: IntArray = intArrayOf(size.x)
    //private val yFood: IntArray = intArrayOf(size.y)
    //private var foodPheromones: Array<IntArray> = arrayOf(xFood, yFood)
    var foodPheromones: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }


    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to start a game with the StartGameMessage
        *   - you need to initialize your ants, they don't know where they start
        *   - here you should manage the pheromones dropped by your ants
        *   - REMEMBER: pheromones should transpire, so old routes get lost
        *   - adjust your parameters to get better results, i.e. amount of ants (capped at 40)
        */
        system.resolve("server") tell StartGameMessage(envId, intialize())

        on { message: StartGameResponse ->
            size = message.size
            nestPosition = message.nestPosition
            obstacles = message.obstacles as ArrayList<Position>?

            nestPheromones = Array(size.x) { Array(size.y) { 0.0 } }
            foodPheromones = Array(size.x) { Array(size.y) { 0.0 } }
            for (ant in antAgentsId) {
                system.resolve(ant) tell EnvironmentSetUpAntMessage(nestPosition)
            }
        }

        listen(BROADCAST_TOPIC) { message: GameTurnInform ->
            log.info("GameTurnInfo-Env: " + message.gameTurn)
            updatePheromones(foodPheromones, 0.1)
            updatePheromones(nestPheromones, 0.1)

            for (ant in antAgentsId) {
                system.resolve(ant) tell AntTurnInformation(message.gameTurn)
            }

        }
        on { message: PheromoneMessage ->
            log.info("PheromoneMessage-Env: " + message.position)
            if (message.boolNestPheromone) {
                nestPheromones[message.position.x][message.position.y] += message.amount
            } else {
                foodPheromones[message.position.x][message.position.y] += message.amount
            }

        }

        respond<InspectPheromoneEnvironmentMessage, ReturnPheromoneEnvironmentMessage> { message ->
            log.info("InspectPheromoneMessage-Env: " + message.position)
            val possiblePos: ArrayList<Position> = getPossiblePositions(message.position, message.boolNestPheromone)

            ReturnPheromoneEnvironmentMessage(possiblePos.get(0), possiblePos.get(1), possiblePos.get(2))
        }

        on {message: EndGameMessage ->
            system.terminate()
        }

    }

    private fun intialize(): List<String> {
        var antNumber = 0
        while (antNumber < numberOfAnts) {
            system.spawnAgent(AntAgent(antNumber.toString()), null, antNumber.toString())
            antAgentsId.add(antNumber.toString())
            antNumber++
        }
        return antAgentsId
    }

    private fun updatePheromones(a: Array<Array<Double>>, x: Double): Void? {
        for (i in 0 until a.size) {
            for (j in 0 until a[i].size) {
                if (a[i][j] != 0.0) {
                    a[i][j] -= x
                }
            }
        }
        return null
    }

    private fun getPossiblePositions(antPosition: Position, useNestPheromone: Boolean): ArrayList<Position> {
        var positionList: ArrayList<Position> = ArrayList()

        if (antPosition.x + 1 <= size.x) {
            positionList.add(Position(antPosition.x + 1, antPosition.y))
            if (antPosition.y + 1 <= size.y) positionList.add(Position(antPosition.x + 1, antPosition.y + 1))
            if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x + 1, antPosition.y - 1))
        }
        if (antPosition.x - 1 >= 0) {
            positionList.add(Position(antPosition.x - 1, antPosition.y))
            if (antPosition.y + 1 <= size.y) positionList.add(Position(antPosition.x - 1, antPosition.y + 1))
            if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x - 1, antPosition.y - 1))
        }

        if (antPosition.y + 1 <= size.y) positionList.add(Position(antPosition.x, antPosition.y + 1))
        if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x, antPosition.y - 1))

        var sortedList: ArrayList<Position> = ArrayList()
        for (position: Position in positionList) {
            if (sortedList.size == 0) sortedList.add(position)
            else {
                for (sortPos: Position in sortedList) {
                    if (comparePosition(sortPos, position, useNestPheromone) >= 0){
                        sortedList.add(position)
                        break
                    }
                }
            }

        }

         return sortedList.subList(0,2) as ArrayList<Position>
    }

    private fun getMapValForPosition(position: Position, useNestPheromone: Boolean): Double {
        var map: Array<Array<Double>> = nestPheromones
        if (!useNestPheromone) map = foodPheromones
        return map[position.x][position.y]

    }

    private fun comparePosition(position1: Position, position2: Position, useNestPheromone: Boolean): Double {
        return getMapValForPosition(position2, useNestPheromone) - getMapValForPosition(position1, useNestPheromone)
    }
}

