package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class CollectAgent(collectID: String, obstacles: List<Position>?, repairPoints: List<Position>, size: Position) :
    Agent(overrideName = collectID) {
    /* TODO
        - this WorkerAgent has the ability to collect material
        - NOTE: can not walk on open repairpoints, can not drop material
        - find material, collect it, start a cnp instance
        - once your cnp is done, meet the RepairAgents and transfer the material
     */
    var hasMaterial: Boolean = false
    var standsOnMaterial: Boolean = false
    val collectID = collectID
    val obstacles = obstacles
    val repairPoints = repairPoints
    val size = size
    var knownMaterial: ArrayList<Position> = ArrayList()
    var meetingPosition: Position? = null
    private val msgBroker by resolve<BrokerAgentRef>()
    var myPosition: Position = Position(0, 0)
    var repairAgentId: String? = null
    var cnpResponses: ArrayList<CNPResponse> = ArrayList()
    var materialPositions: ArrayList<Position> = ArrayList()

    override fun behaviour() = act {
        on<CurrentPosition> { message ->
            myPosition = message.position
            doTurn(message.position, message.vision)
        }

        on<CNPResponse> { message ->
            cnpResponses.add(message)
        }

        on<MaterialPositions> { message ->
            materialPositions.add(message.position)
        }
    }

    private fun doTurn(position: Position, vision: List<Position>) {
        log.info(collectID + vision)
        val ref = system.resolve(SERVER_NAME)
        if (!hasMaterial && !vision.isEmpty() && !standsOnMaterial) {
            doMaterialMove(position, vision)
        } else if (standsOnMaterial && !hasMaterial) {
            log.info(collectID + " takes material")
            msgBroker.publish(MATERIAL_TOPIC, MaterialPositions(position, true))
            ref invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, WorkerAction.TAKE)) {
                if (it.state) {
                    hasMaterial = true
                    standsOnMaterial = false
                    log.info(collectID + " has taken Material")
                } else {
                    considerActionFlags(it, position, vision)
                }
            }
        } else if (hasMaterial) {
            meetAndFindRepairAgent()
            log.info(collectID + " should do CNP now")
        } else {
            doMove(position, vision)
        }
    }

    private fun getPossiblePositions(currentPosition: Position): ArrayList<Position> {

        val positions = ArrayList<Position>()

        if (currentPosition.x + 1 < size.x) {
            positions.add(Position(currentPosition.x + 1, currentPosition.y))
            if (currentPosition.y + 1 < size.y) positions.add(
                Position(
                    currentPosition.x + 1,
                    currentPosition.y + 1
                )
            )
            if (currentPosition.y - 1 >= 0) positions.add(
                Position(
                    currentPosition.x + 1,
                    currentPosition.y - 1
                )
            )
        }
        if (currentPosition.x - 1 >= 0) {
            positions.add(Position(currentPosition.x - 1, currentPosition.y))
            if (currentPosition.y + 1 < size.y) positions.add(
                Position(
                    currentPosition.x - 1,
                    currentPosition.y + 1
                )
            )
            if (currentPosition.y - 1 >= 0) positions.add(
                Position(
                    currentPosition.x - 1,
                    currentPosition.y - 1
                )
            )
        }

        if (currentPosition.y + 1 < size.y) positions.add(Position(currentPosition.x, currentPosition.y + 1))
        if (currentPosition.y - 1 >= 0) positions.add(Position(currentPosition.x, currentPosition.y - 1))

        for (position in positions) {
            if (repairPoints.contains(position)) {
                positions.remove(position)
                continue
            }
            if (obstacles != null && obstacles.contains(position)) {
                positions.remove(position)
                continue
            }
        }

        return positions
    }

    private fun getActionForPosition(currentPosition: Position, nextPosition: Position): WorkerAction {
        if (currentPosition.x == nextPosition.x) {
            if (nextPosition.y > currentPosition.y) return WorkerAction.SOUTH
            if (nextPosition.y < currentPosition.y) return WorkerAction.NORTH
        }
        if (currentPosition.y == nextPosition.y) {
            if (nextPosition.x > currentPosition.x) return WorkerAction.EAST
            if (nextPosition.x < currentPosition.x) return WorkerAction.WEST
        }
        if (currentPosition.x > nextPosition.x && nextPosition.y > currentPosition.y) return WorkerAction.SOUTHWEST
        if (currentPosition.x > nextPosition.x && nextPosition.y < currentPosition.y) return WorkerAction.NORTHWEST

        if (currentPosition.x < nextPosition.x && nextPosition.y > currentPosition.y) return WorkerAction.SOUTHEAST
        if (currentPosition.x < nextPosition.x && nextPosition.y < currentPosition.y) return WorkerAction.NORTHEAST
        //failure
        return WorkerAction.TAKE
    }

    private fun getClosestMaterialSource(currentPosition: Position): Position {
        var minDistance = 0
        var positionOfMinDistance = currentPosition
        for (materialPosition in materialPositions) {
            val distance = calculateDistance(currentPosition, materialPosition)
            if (distance < minDistance) {
                minDistance = distance
                positionOfMinDistance = materialPosition
            }
        }
        return positionOfMinDistance
    }

    private fun getAdjacentPositions(position: Position): List<Position> {
        val adjacentPositions = mutableListOf<Position>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue  // Skip the current position
                adjacentPositions.add(Position(position.x + dx, position.y + dy))
            }
        }
        return adjacentPositions
    }

    private fun isPositionValid(position: Position, obstacles: List<Position>?): Boolean {
        if (obstacles != null) {
            return !obstacles.contains(position)
        }
        return true
    }

    private fun getNextPositionToGoal(currentPosition: Position, goal: Position): Position{
        val openList = mutableListOf<RepairAgent.Node>()
        val closedList = mutableSetOf<RepairAgent.Node>()

        val startNode = RepairAgent.Node(currentPosition, 0, calculateDistance(currentPosition, goal))
        openList.add(startNode)

        while (openList.isNotEmpty()) {
            val currentNode = openList.minBy { it.fCost }
            openList.remove(currentNode)
            currentNode?.let {
                closedList.add(it)

                val adjacentPositions = getAdjacentPositions(it.position)
                for (adjacentPos in adjacentPositions) {
                    if (!isPositionValid(adjacentPos, obstacles) || closedList.any { node -> node.position == adjacentPos }) {
                        continue
                    }

                    val gCost = it.gCost + calculateDistance(it.position, adjacentPos)
                    val hCost = calculateDistance(adjacentPos, goal)
                    val newNode = RepairAgent.Node(adjacentPos, gCost, hCost, it)

                    val existingNode = openList.find { node -> node.position == newNode.position }
                    if (existingNode == null || existingNode.fCost > newNode.fCost) {
                        existingNode?.let { node -> openList.remove(node) }
                        openList.add(newNode)
                    }
                }
            }
        }

        // Get the best next position based on fCost
        val bestNode = closedList.minBy { it.fCost }
        if (bestNode != null) {
            return bestNode.position
        }
        // No path found
        else {
            var possiblePositions = getPossiblePositions(currentPosition)
            return possiblePositions.get(Random.nextInt(0, possiblePositions.size - 1))
        }
    }

    private fun doMove(currentPosition: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        var possiblePositions = getPossiblePositions(currentPosition)
        val nextPosition: Position

        // if anny materials in vision go there else go 1 step closer to the closest material source
        if (vision.isNotEmpty()) {
            nextPosition = vision.get(Random.nextInt(0, vision.size - 1))
        }
        else if (materialPositions.isNotEmpty()) {
            nextPosition = getNextPositionToGoal(currentPosition, getClosestMaterialSource(currentPosition))
        }
        else {
            nextPosition = possiblePositions.get(Random.nextInt(0, possiblePositions.size - 1))
        }

        var nextAction = getActionForPosition(currentPosition, nextPosition)

        ref invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, nextAction)) {
            if (!it.state) {
                considerActionFlags(it, currentPosition, vision)
            }
        }
    }

    private fun considerActionFlags(message: WorkerActionResponse, position: Position, vision: List<Position>) {
        when (message.flag) {
            ActionFlag.MAX_ACTIONS -> log.info(collectID + ": Max Action")
            ActionFlag.HAS_MATERIAL -> log.info(collectID + ": Has Material")
            ActionFlag.NO_ACTIVE_GAME -> log.info(collectID + ": No Active Game")
            ActionFlag.OBSTACLE -> doMove(position, vision)
            ActionFlag.NONE -> log.info(collectID + ": None")
            ActionFlag.NO_MATERIAL -> doMaterialMove(position, vision)
        }
    }

    private fun doMaterialMove(position: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        val nextAction = getActionForPosition(position, vision.first())
        ref invoke ask<WorkerActionResponse>(WorkerActionRequest(this.collectID, nextAction)) {
            if (it.state) {
                standsOnMaterial = true
                log.info(collectID + " stands on Material")
            } else {
                considerActionFlags(it, position, vision)
            }
        }
    }

    private fun meetAndFindRepairAgent() {
        if (meetingPosition == null) {
            doCNP()
        } else if (meetingPosition != myPosition) {
            // move to position
        } else {
            // transfer material
            if (repairAgentId != null){
                system.resolve(SERVER_NAME) tell TransferMaterial(collectID, repairAgentId!!)
            } else {
                log.info(collectID + ": tried to transfer Material, no repairAgentId")
            }
        }
    }

    private fun doCNP() {
        msgBroker.publish(CNP_TOPIC, CNPRequest(collectID, myPosition))

        Timer().schedule(100) {
            val bestMessage = getBestMeetingPoint()
            if (bestMessage != null) {
                for (message in cnpResponses) {
                    if (message == bestMessage) {
                        system.resolve(bestMessage.repairAgentId) invoke ask<InformCancelCNP>(AcceptRejectCNP(true)) {
                            if (it.accepted) {
                                meetingPosition = bestMessage.meetingPosition
                                //TODO: move to position
                            } else {
                                //TODO: vielleicht bessere Fehlerbehandlung möglich
                                doCNP()
                                log.info(collectID + ": repairAgent rejected")
                            }
                        }
                    } else {
                        //für alle anderen rejected Agenten
                        system.resolve(message.repairAgentId) tell AcceptRejectCNP(false)
                    }
                }
            } else {
                log.info(collectID + ": error no messages")
            }
        }
    }

    private fun getBestMeetingPoint(): CNPResponse? {
        var bestCNPResponse: CNPResponse? = null
        var bestVal: Int = -1
        for (response in cnpResponses) {
            if (bestCNPResponse == null) {
                bestCNPResponse = response
                bestVal = calculateDistance(myPosition, response.meetingPosition)
            } else {
                val newVal = calculateDistance(myPosition, response.meetingPosition)
                if (newVal < bestVal) {
                    bestVal = newVal
                    bestCNPResponse = response
                }
            }
        }
        return bestCNPResponse
    }

}