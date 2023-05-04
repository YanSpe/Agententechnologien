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

            for (ant in antAgentsId) {
                system.resolve(ant) tell EnvironmentSetUpAntMessage(nestPosition)
            }

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
}