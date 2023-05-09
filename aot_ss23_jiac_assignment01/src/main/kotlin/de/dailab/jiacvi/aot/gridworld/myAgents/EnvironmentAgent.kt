package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String): Agent(overrideName=envId) {
    // TODO you might need to put some variables to save stuff here

    private val numberOfAnts: Int = 3
    private val antAgentsId: ArrayList<String> = ArrayList()

    private lateinit var size: Position
    private lateinit var nestPosition: Position
    private var obstacles: ArrayList<Position>? = null

    //private val xNest: IntArray = intArrayOf(size.x)
    //private val yNest: IntArray = intArrayOf(size.y)
    //private val nestPheromones: Array<IntArray> = arrayOf(xNest, yNest)
    var nestPheromones: Array<Array<Double>> = Array(1) {Array(1) {0.0} }
    //private val xFood: IntArray = intArrayOf(size.x)
    //private val yFood: IntArray = intArrayOf(size.y)
    //private var foodPheromones: Array<IntArray> = arrayOf(xFood, yFood)
    var foodPheromones: Array<Array<Double>> = Array(1) {Array(1) {0.0} }


    override fun preStart() {
        // TODO if you want you can do something once before the normal lifecycle of your agent
        super.preStart()
    }

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

            nestPheromones = Array(size.x) {Array(size.y) {0.0} }
            foodPheromones = Array(size.x) {Array(size.y) {0.0} }
            for (ant in antAgentsId) {
                system.resolve(ant) tell EnvironmentSetUpAntMessage(nestPosition)
            }
        }

        on { message: GameTurnInform ->

            updatePheromones(foodPheromones, 0.1)
            updatePheromones(nestPheromones, 0.1)

            for (ant in antAgentsId) {
                system.resolve(ant) tell AntTurnInformation(message.gameTurn)
            }


        }
        on {message: PheromoneMessage ->
            if(message.boolNestPheromone){
                nestPheromones[message.position.x][message.position.y] += message.amount
            }
            else
            {
                foodPheromones[message.position.x][message.position.y] += message.amount
            }

        }

        respond<InspectPheromoneEnvironmentMessage, ReturnPheromoneEnvironmentMessage> { message ->
            var possiblePos: ArrayList<Position> = getPossiblePositions(message.position, message.boolNestPheromone)


            ReturnPheromoneEnvironmentMessage()
        }

    }

    private fun intialize(): List<String> {
        var antNumber = 0
        while (antNumber < numberOfAnts) {
            system.spawnAgent(AntAgent(antNumber.toString()),null,antNumber.toString())
            antAgentsId.add(antNumber.toString())
            antNumber++
        }
        return antAgentsId
    }

    private fun updatePheromones(a : Array<Array<Double>>, x : Double): Void? {
        for (i in 0 until a.size) {
            for (j in 0 until a[i].size) {
                if(a[i][j] != 0.0) {
                    a[i][j] -= x
                }
            }
        }
        return null
    }

    private fun getPossiblePositions(antPosition: Position, useNestPheromone: Boolean): ArrayList<Position>{
        var map: Array<Array<Double>> = nestPheromones
        if(!useNestPheromone) map = foodPheromones

        var returnList: ArrayList<Position> = ArrayList()



        return returnList
    }
}

