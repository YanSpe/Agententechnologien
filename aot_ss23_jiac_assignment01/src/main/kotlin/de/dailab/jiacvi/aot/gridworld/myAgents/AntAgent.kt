package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*

/**
 * Stub for your AntAgent
 * */
class AntAgent(antId: String): Agent(overrideName=antId) {
    // TODO you might need to put some variables to save stuff here
    var position: Position? = null
    var holdingFood: Boolean = false
    var antId: String = antId
    var atFood: Boolean = false

    fun doAction() {
        //val random = Random() // 1-7 generieren
        //var action: AntAction = (0 until 8).random()
        var action: AntAction = AntAction.values().toList().shuffled().first() // remove drop food etc
        if (holdingFood) {
            // Pheromone
        } else {
            // pheromone
        }
        system.resolve("server") tell AntActionRequest(antId, action)

    }
    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to make a move in the gridworld
        *   - build your ant algorithm by communicating with your environment when looking for the way
        *   - adjust your parameters to get better results
        */

        on {    message: EnvironmentSetUpAntMessage ->
            position = message.position
        }

        on { message: AntTurnInformation ->
          doAction()
        }

        on { message: AntActionResponse ->
            when (message.flag){
                ActionFlag.NO_ACTIVE_GAME -> println("Error")  // ant is not registered or no game started
                ActionFlag.MAX_ACTIONS -> println("too many actions")    // ants can only do 1 action per turn
                ActionFlag.OBSTACLE -> doAction()       // border of grid or obstacle (#) in grid
                ActionFlag.NO_FOOD -> println("no food")        // ant has no food to drop or is not at active food source to take
                ActionFlag.NO_NEST -> println("no nest")        // ant is not at nest while trying to drop
                ActionFlag.HAS_FOOD ->  atFood = true      // new position is active food source or ant has food and can't take more
                ActionFlag.NONE -> println("none")
            }

        }





    }
}
