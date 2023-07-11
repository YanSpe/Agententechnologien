package de.dailab.jiacvi.aot.gridworld.myAgents

import com.google.protobuf.Empty
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
        system.resolve(SERVER_NAME) invoke ask<SetupGameResponse>(SetupGameMessage(setupID, "/grids/Fortress_grp11.grid")) { message ->
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

data class CNPRequest(val collectAgentId: String, val workerPosition: Position)
data class CNPResponse(val repairAgentId: String, val meetingPosition: Position)
data class AcceptRejectCNP(val accepted: Boolean)
data class InformCancelCNP(val accepted: Boolean)
data class RepairPointsUpdate(val RepairPoints: MutableList<Position>)

/** CollectAgent -> CollectAgent (tell), Position of material if empty remove from list*/
data class MaterialPositions(val position: Position, val materialThere: Boolean)

data class RepairAgentArrivedOnCNPMeetingPosition(val repairAgentId: String, val Arrived: Boolean)

const val CNP_TOPIC = "cnp"
const val REPAIR_POINTS = "repairPoints"
const val AVAILABLE_REPAIR_POINTS = "availableRepairPoints"
const val MATERIAL_TOPIC = "materialPositions"

fun calculateDistance(start: Position, end: Position): Int {
    val dx = kotlin.math.abs(start.x - end.x)
    val dy = kotlin.math.abs(start.y - end.y)
    return dx + dy
}
