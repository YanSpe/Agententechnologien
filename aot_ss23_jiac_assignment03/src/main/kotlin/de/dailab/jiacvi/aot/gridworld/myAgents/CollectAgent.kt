package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.random.Random

class CollectAgent(collectID: String, obstacles: List<Position>?, repairPoints: List<Position>, size: Position) :
    Agent(overrideName = collectID) {
    var hasMaterial: Boolean = false
    var standsOnMaterial: Boolean = false
    val collectID = collectID
    val obstacles = obstacles
    val repairPoints = repairPoints
    val size = size
    var meetingPosition: Position? = null
    private val msgBroker by resolve<BrokerAgentRef>()
    var myPosition: Position = Position(0, 0)
    var repairAgentId: String? = null
    var cnpBids: ArrayList<CNPBid> = ArrayList()
    var materialPositions: ArrayList<Position> = ArrayList()
    var partnerAgentIsOnMeetingPoint: Boolean = false
    var definitiveBid: DefinitiveBid? = null
    var startedCNP: Boolean = false
    var last_turn_no_message = 0

    override fun behaviour() = act {
        on<CurrentPosition> { message ->
            myPosition = message.position
            doTurn(message.position, message.vision)
        }

        on<CNPBid> { message ->
            log.info(collectID + ": got Bid " + message)
            cnpBids.add(message)
        }

        on<DefinitiveBid> {
            log.info(collectID + ": got Definitive Bid")
            definitiveBid = it
        }

        on<RestartCNPMessage> {
            definitiveBid = null
            log.info(collectID + ": cnpBids-löscht-46")
            cnpBids = ArrayList()
            meetingPosition = null
            repairAgentId = null
            startedCNP = false
            meetAndFindRepairAgent()
        }

        on<MaterialPositions> { message ->
            if (message.materialThere) {
                materialPositions.add(message.position)
            } else {
                materialPositions.remove(message.position)
            }
        }

        on<RepairAgentArrivedOnCNPMeetingPosition> { message ->
            log.info(collectID + ": got Meeting-Arrive-Message from " + message.repairAgentId)
            partnerAgentIsOnMeetingPoint = true
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
                    //log.info(collectID + " has taken Material")
                } else {
                    considerActionFlags(it, position, vision)
                }
            }
        } else if (hasMaterial) {
            meetAndFindRepairAgent()

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

        val returnPositions = ArrayList<Position>()
        for (position in positions) {
            if (repairPoints.contains(position) || (obstacles != null && obstacles.contains(position))) {
                continue
            } else {
                returnPositions.add(position)
            }
        }

        return returnPositions
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

    private fun getNextPositionToGoal(currentPosition: Position, goal: Position): Position {
        // A* Algorithmus
        val openList = mutableListOf<RepairAgent.Node>()
        val closedList = mutableSetOf<RepairAgent.Node>()

        val startNode = RepairAgent.Node(currentPosition, 0, calculateDistance(currentPosition, goal))
        openList.add(startNode)

        while (openList.isNotEmpty()) {
            val currentNode = openList.minBy { it.fCost } ?: break
            openList.remove(currentNode)
            closedList.add(currentNode)


            if (currentNode.position == goal) {
                // Reached the end position
                val path = mutableListOf<Position>()
                var node = currentNode
                while (node.parent != null) {
                    path.add(node.position)
                    node = node.parent!!
                }
                path.reverse()
                return path[0]
            }

            val adjacentPositions = getAdjacentPositions(currentNode.position)
            for (adjacentPos in adjacentPositions) {
                if (!isPositionValid(
                        adjacentPos,
                        obstacles
                    ) || closedList.any { node -> node.position == adjacentPos }
                ) {
                    continue
                }

                val gCost = currentNode.gCost + calculateDistance(currentNode.position, adjacentPos)
                val hCost = calculateDistance(adjacentPos, goal)
                val newNode = RepairAgent.Node(adjacentPos, gCost, hCost, currentNode)

                val existingNode = openList.find { node -> node.position == newNode.position }
                if (existingNode == null || existingNode.fCost > newNode.fCost) {
                    existingNode?.let { node -> openList.remove(node) }
                    openList.add(newNode)
                }
            }
        }

        // Get the best next position based on fCost
        if (closedList.isNotEmpty()) {
            val bestNode = closedList.minBy { it.fCost }
            if (bestNode != null) {
                return bestNode.position
            }
        }

        // No path found
        return currentPosition
    }

    private fun doMove(currentPosition: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        val possiblePositions = getPossiblePositions(currentPosition)
        val nextPosition: Position

        // if anny materials in vision go there else go 1 step closer to the closest material source
        if (materialPositions.isNotEmpty()) {
            nextPosition = getNextPositionToGoal(currentPosition, getClosestMaterialSource(currentPosition))
        } else {
            var random = 0
            if (possiblePositions.size > 1) {
                //log.info(collectID + ": PossiblePositions.Size=" + possiblePositions.size)
                random = Random.nextInt(0, possiblePositions.size - 1)
            } else {
                //log.info(collectID + "possiblePositions Size=" + possiblePositions.size)
            }
            nextPosition = possiblePositions.get(random)
        }

        val nextAction = getActionForPosition(currentPosition, nextPosition)

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
            ActionFlag.OBSTACLE -> {log.info(collectID + ": Obstacle")}
            ActionFlag.NONE -> log.info(collectID + ": None")
            ActionFlag.NO_MATERIAL -> {
                standsOnMaterial = false; log.info(collectID + ": No Material")
            }

            else -> {
                log.info("ActionFlag Error")
            }
        }
    }

    private fun doMaterialMove(position: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        val nextAction = getActionForPosition(position, vision.first())
        ref invoke ask<WorkerActionResponse>(WorkerActionRequest(this.collectID, nextAction)) {
            if (it.state) {
                standsOnMaterial = true
                // log.info(collectID + " stands on Material")
            } else {
                considerActionFlags(it, position, vision)
            }
        }
    }

    private fun meetAndFindRepairAgent() {
        // there is no meeting position
        if (meetingPosition == null) {
            doCNP()
        }
        // collector agent is not at the meeting position
        else if (meetingPosition != myPosition) {
            // move to position
            val nextPosition = getNextPositionToGoal(myPosition, meetingPosition!!)
            val nextAction = getActionForPosition(myPosition, nextPosition)
            system.resolve(SERVER_NAME) invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, nextAction)) {
                if (!it.state) {
                    //log.info(collectID + ": error in Moving to Meeting-Position")
                }
            }
        }
        // collector agent is at the meeting position
        else {
            log.info(collectID + ": is on MeetingPosition")
            // transfer material
            if (repairAgentId != null) {
                if (partnerAgentIsOnMeetingPoint) {
                    system.resolve(SERVER_NAME) tell TransferMaterial(collectID, repairAgentId!!)
                    hasMaterial = false
                    repairAgentId = null
                    partnerAgentIsOnMeetingPoint = false
                    meetingPosition = null
                } else {
                    log.info(collectID + ": waiting to send Transfer" + ";myPosition: " + myPosition + "; meetinPoint: " + meetingPosition + "; partnerIsOnMeetingPoint: " + partnerAgentIsOnMeetingPoint + "; partnerId: "+ repairAgentId)
                }
            } else {
                log.info(collectID + ": is on MeetingPosition without repairAgentId")
            }
        }
    }

    private fun doCNP() {
        if (!startedCNP || (last_turn_no_message >= 3 && meetingPosition == null)) {
            log.info(collectID + " should do CNP now")
            msgBroker.publish(CNP_TOPIC, CNPRequest(collectID, myPosition))
            startedCNP = true
            last_turn_no_message = 0
        }

        var endWhileLoop = false
        while (!endWhileLoop && meetingPosition == null) {
            //log.info(collectID + "-MeetingPosition: " +meetingPosition)
            //TODO: warten auf alle BIDS
            if (definitiveBid != null) {
                log.info(collectID + ": got Definitive Bid")
                //log.info(collectID + ": Timer ended; is Working on State 5")
                val orderedResponses = getOrderedCNPResponseList()
                if (orderedResponses.isEmpty()) {
                    system.resolve(definitiveBid!!.repairAgentId) tell DefinitiveAcceptRejectCNP(
                        true,
                        definitiveBid!!.meetingPosition,
                        collectID
                    )
                    log.info(collectID + ": definitive accepted Bid from " + definitiveBid!!.repairAgentId + " and and was alone")
                    meetingPosition = definitiveBid!!.meetingPosition
                    repairAgentId = definitiveBid!!.repairAgentId
                    startedCNP = false
                    log.info(collectID + ": meetingPosition set to " + definitiveBid!!.meetingPosition)
                } else {
                    val meetingDistanceBid = calculateDistance(myPosition, orderedResponses.first().meetingPosition)
                    val meetingDistanceDefinitiveBid =
                        calculateDistance(myPosition, definitiveBid!!.meetingPosition)

                    if (meetingDistanceBid < meetingDistanceDefinitiveBid) {
                        // go to State 2 --> reject Definitive Bid and Accept best Bid
                        system.resolve(definitiveBid!!.repairAgentId) tell AcceptReject(
                            false,
                            collectID,
                            myPosition
                        )
                        for (message in orderedResponses) {
                            if (message == orderedResponses.first()) {
                                system.resolve(message.repairAgentId) tell AcceptReject(true, collectID, myPosition)
                                log.info(collectID + ": send accept message to " + message.repairAgentId)
                            } else {
                                system.resolve(message.repairAgentId) tell AcceptReject(
                                    false,
                                    collectID,
                                    myPosition
                                )
                                log.info(collectID + ": send reject message to " + message.repairAgentId)
                            }
                        }
                    } else {
                        // DefinitveAccept Defintive Bid
                        system.resolve(definitiveBid!!.repairAgentId) tell DefinitiveAcceptRejectCNP(
                            true,
                            definitiveBid!!.meetingPosition,
                            collectID
                        )
                        log.info(collectID + ": definitive accepted Bid from " + definitiveBid!!.repairAgentId + " and definitive rejected everyone else")
                        meetingPosition = definitiveBid!!.meetingPosition
                        repairAgentId = definitiveBid!!.repairAgentId
                        startedCNP = false
                        for (message in orderedResponses) {
                            system.resolve(message.repairAgentId) tell DefinitiveAcceptRejectCNP(
                                false,
                                myPosition,
                                collectID
                            )
                        }
                    }
                }
                //log.info(collectID + ": cnpBids-löscht-406")
                cnpBids = ArrayList()
                definitiveBid = null
            } else {
                log.info(collectID + ": Definitive Bid is Null")
                val orderedResponses = getOrderedCNPResponseList()
                if (orderedResponses.isNotEmpty()) {
                    for (message in orderedResponses) {
                        if (message == orderedResponses.first()) {
                            system.resolve(message.repairAgentId) tell AcceptReject(true, collectID, myPosition)
                            log.info(collectID + ": send accept message to " + message.repairAgentId)
                        } else {
                            system.resolve(message.repairAgentId) tell AcceptReject(false, collectID, myPosition)
                            log.info(collectID + ": send reject message to " + message.repairAgentId)
                        }
                    }
                    //log.info(collectID + ": cnpBids-löscht-421")
                    cnpBids = ArrayList()
                } else {
                    last_turn_no_message += 1
                    //log.info(collectID + ": error no messages")
                    endWhileLoop = true
                }
            }
        }
    }

    private fun getOrderedCNPResponseList(): List<CNPBid> {
        //log.info(collectID + ": cnpBids " + cnpBids)
        if (cnpBids.isEmpty()) {
            return emptyList()
        }
        return cnpBids.sortedBy { calculateDistance(it.meetingPosition, myPosition) }
    }

}