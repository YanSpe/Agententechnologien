package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String): Agent(overrideName=envId) {
    // TODO you might need to put some variables to save stuff here





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
    }
}