package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import kotlin.system.exitProcess

class SetupAgent (private val setupID: String): Agent(overrideName=setupID) {


    /* TODO
        - setup the game using the SetupGameMessage, you need to define a gridfile
        - use the list of ids to spawn Repair & Collect Agents i.e. system.spawn(CollectAgent("x"))
        - if you need to, do some more prep work
        - start the game by telling the server "StartGame(setupID)"
     */

    override fun preStart() {
        system.resolve(SERVER_NAME) invoke ask<SetupGameResponse>(SetupGameMessage(setupID, "/grids/example.grid")) { message ->
            for (collectorId in message.collectorIDs) {
                system.spawnAgent(CollectAgent(collectorId, message.obstacles, message.repairPoints, message.size))
            }
            for (repairId in message.repairIDs) {
                system.spawnAgent(RepairAgent(repairId, message.obstacles, message.repairPoints, message.size))
            }

            system.resolve(SERVER_NAME) invoke ask<Boolean>(StartGame(setupID)){}

        }
    }

    override fun behaviour() = act {



        on<EndGameMessage> {
            log.info("Received $it")
            exitProcess(0)
        }
    }
}