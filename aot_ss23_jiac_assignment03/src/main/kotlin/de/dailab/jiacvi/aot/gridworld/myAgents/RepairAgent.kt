package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act

class RepairAgent(repairID: String, obstacles: List<Position>?, repairPoints: List<Position>, size: Position): Agent(overrideName=repairID) {
    /* TODO
        - this WorkerAgent has the ability to drop material
        - NOTE: can walk on open repairpoints, can not collect material
        - participate in cnp instances, meet with CollectAgent, get material
        - go to repairpoint, drop material
     */
    var hasMaterial: Boolean = false
    var standsOnRepairPoint: Boolean = false
    val obstacles = obstacles
    var repairPoints: MutableList<Position> = repairPoints.toMutableList()
    var availableRepairPoints: MutableList<Position> = repairPoints.toMutableList()
    var myRepairPoint: Position = Position(-1,-1)
    val size = size
    val repairID = repairID
    lateinit var repairAgentPosition: Position
    private val msgBroker by resolve<BrokerAgentRef>()
    var CNP_active: Boolean = false
    lateinit var CNP_meetingPosition: Position
    lateinit var CNP_collectAgentId: String

    override fun behaviour() = act {
        // Get current Position from Server, start doTurn logic
        on<CurrentPosition> { message ->
            repairAgentPosition = message.position
            standsOnRepairPoint = repairPoints.contains(repairAgentPosition)
            doTurn(message.position, message.vision)
        }

        // On message in CNP_TOPIC start CNP logic if CNP is not already active
        listen<CNPRequest>(CNP_TOPIC){ message ->
            log.info(repairID + ": received CNP Request")
            if(!CNP_active && !hasMaterial){
                doCNP(message.collectAgentId, message.workerPosition)
            }
        }

        // React to CNP Acceptance or Rejection from CollectAgent
        respond<AcceptRejectCNP, InformCancelCNP> { message ->
            log.info(repairID + ": got CNP-Response " + message)
            if (message.accepted){
                CNP_active = true
                return@respond InformCancelCNP(true)
            }
            return@respond InformCancelCNP(false)
        }

        // React to successful transfer of material
        on<TransferInform> {message ->
            hasMaterial = true
            CNP_active = false
            CNP_collectAgentId = ""
            log.info("Transfer successful")
        }

        // Update repair points if another repair agent publishes new information
        listen<RepairPointsUpdate>(REPAIR_POINTS){ message ->
            repairPoints = message.RepairPoints
            standsOnRepairPoint = repairPoints.contains(repairAgentPosition)
            if (!repairPoints.contains(myRepairPoint) && myRepairPoint != Position(-1,-1)){
                myRepairPoint = Position(-1,-1)
            }
            log.info(repairID + " received repair point update")
        }

        // Update available repair points if another repair agent publishes new information
        listen<RepairPointsUpdate>(AVAILABLE_REPAIR_POINTS){ message ->
            availableRepairPoints = message.RepairPoints
            //log.info(repairID + " received available repair point update")
        }
    }

    // Logic for the next turn (repair, move towards meeting point/repair point, wait for CNP partner)
    private fun doTurn(position: Position, vision: List<Position>) {
        //log.info(repairID + vision)
        val ref = system.resolve(SERVER_NAME)
        // Repair
        if (standsOnRepairPoint && hasMaterial) {
            log.info(repairID + " drops material")
            ref invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, WorkerAction.DROP)) {
                if (it.state) {
                    // update variables after successful repair
                    log.info(repairID + " has repaired a hole")
                    hasMaterial = false
                    standsOnRepairPoint = false
                    repairPoints.remove(repairAgentPosition)
                    msgBroker.publish(REPAIR_POINTS, RepairPointsUpdate(repairPoints))
                    myRepairPoint = Position(-1,-1)
                } else {
                    considerActionFlags(it, position, vision)
                }
            }
        }
        // Wait for Material transfer
        else if (!hasMaterial && CNP_active && repairAgentPosition == CNP_meetingPosition){
            system.resolve(CNP_collectAgentId) tell (RepairAgentArrivedOnCNPMeetingPosition(repairID, true))
            log.info(repairID + " waits for material transfer and has send Message")
        }
        // Wait for CNP
        else if (!hasMaterial && standsOnRepairPoint && !CNP_active){
            log.info(repairID + " waits for CNP")
        }
        else {
            // Move to Position depending on if CNP is active or not
            //log.info(repairID + " makes a move")
            doMove(position, vision)
        }
    }

    // Finds meeting position and sends it to collect agent
    private fun doCNP(collectAgentId: String, collectAgentPosition: Position){
        CNP_meetingPosition = findMeetingPoint(collectAgentPosition)
        CNP_collectAgentId = collectAgentId
        log.info(repairID+" found meeting point: "+CNP_meetingPosition)
        val ref = system.resolve(collectAgentId)
        //log.info(repairID+" tries to contact "+collectAgentId+" ("+ref+")")
        ref tell (CNPResponse(repairID, CNP_meetingPosition))
    }

    // looks for center between repair and collect agent that isnt an obstacle
    private fun findMeetingPoint(collectAgentPosition: Position): Position{
        var meetingPosition: Position? = null
        var counter: Int = 0

        while (meetingPosition == null){
            if (counter == 0){
                meetingPosition = Position((collectAgentPosition.x+repairAgentPosition.x)/2, (collectAgentPosition.y+repairAgentPosition.y)/2)
                counter++
            } else  {
                meetingPosition = getAdjacentPositions(Position((collectAgentPosition.x+repairAgentPosition.x)/2, (collectAgentPosition.y+repairAgentPosition.y)/2)).random()
            }
           if (!isPositionValid(meetingPosition, obstacles)) {
               meetingPosition = null
               counter++
           }
            if (counter >= 9){
                log.info("could not find meeting position, coming to collectAgentPosition")
                return collectAgentPosition
            }
        }
        return meetingPosition
    }

    // Moves towards repair point or CNP Meeting point
    private fun doMove(position: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        var nextPosition: Position? = null
        if(!hasMaterial && CNP_active) {
            nextPosition = getNextPosition(position, CNP_meetingPosition)
            log.info(repairID + " goes towards meeting point: "+ CNP_meetingPosition)
        } else {
            var nearestRepairPoint: Position = getNearestRepairPoint(position)
            if (nearestRepairPoint != myRepairPoint){
                myRepairPoint = nearestRepairPoint
                log.info("My repair point is: "+myRepairPoint)
                availableRepairPoints.remove(repairAgentPosition)
                msgBroker.publish(AVAILABLE_REPAIR_POINTS, RepairPointsUpdate(availableRepairPoints))
            }

            log.info(repairID + " goes towards nearest repair point at position " + nearestRepairPoint)

            nextPosition = getNextPosition(position, nearestRepairPoint)
            log.info(repairID + " goes to next position: " + nextPosition)
        }
        if (nextPosition != null) {
            var nextAction = getActionForPosition(position, nextPosition)
            log.info(repairID + " does action "+ nextAction)

            ref invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, nextAction)) {
                if (!it.state) {
                    considerActionFlags(it, position, vision)
                }
            }
        }
    }

    // Returns nearest repair point
    private fun getNearestRepairPoint(position: Position): Position {
        if (myRepairPoint != Position(-1,-1)){
            return myRepairPoint
        }
        if (!availableRepairPoints.isEmpty()) {
            return availableRepairPoints.sortedBy { calculateDistance(position, it) }.first()
        }
        return position
    }

    // Adjacent Positions to given position
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

    // Return false if position is obstacle
    private fun isPositionValid(position: Position, obstacles: List<Position>?): Boolean {
        if (obstacles != null) {
            return !obstacles.contains(position)
        }
        return true
    }

    // A* algorithm to find next position
    private fun getNextPosition(position: Position, goal: Position): Position?{
        log.info(repairID + " starts A* with position: "+ position+" goal: "+ goal+" obstacles: "+ obstacles)
        // A* Algorithmus
        val openList = mutableListOf<Node>()
        val closedList = mutableSetOf<Node>()

        val startNode = Node(position, 0, calculateDistance(position, goal))
        openList.add(startNode)

        while (openList.isNotEmpty()) {
            //log.info("openlist while iteration with length: "+ openList.size)
            val currentNode = openList.minBy { it.fCost } ?: break
            openList.remove(currentNode)
            //log.info("openlist length after removal: "+ openList.size)
            closedList.add(currentNode)


            if (currentNode.position == goal) {
                // Reached the end position
                //log.info("reached goal")
                //return it.position
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
                if (!isPositionValid(adjacentPos, obstacles) || closedList.any { node -> node.position == adjacentPos }) {
                    continue
                }

                val gCost = currentNode.gCost + calculateDistance(currentNode.position, adjacentPos)
                val hCost = calculateDistance(adjacentPos, goal)
                val newNode = Node(adjacentPos, gCost, hCost, currentNode)

                val existingNode = openList.find { node -> node.position == newNode.position }
                if (existingNode == null || existingNode.fCost > newNode.fCost) {
                    existingNode?.let { node -> openList.remove(node) }
                    openList.add(newNode)
                }
            }
        }
        log.info("while loop done")

        // No path found
        if (closedList.isEmpty()) {
            log.info("Critical error, A* found no path")
            return null
        }

        // Get the best next position based on fCost
        val bestNode = closedList.minBy { it.fCost }
        return bestNode?.position
    }


    // Convert Position into action
    private fun getActionForPosition(myPosition: Position, nextPosition: Position): WorkerAction {
        if (myPosition.x == nextPosition.x) {
            if (nextPosition.y > myPosition.y) return WorkerAction.SOUTH
            if (nextPosition.y < myPosition.y) return WorkerAction.NORTH
        }
        if (myPosition.y == nextPosition.y) {
            if (nextPosition.x > myPosition.x) return WorkerAction.EAST
            if (nextPosition.x < myPosition.x) return WorkerAction.WEST
        }
        if (myPosition.x > nextPosition.x && nextPosition.y > myPosition.y) return WorkerAction.SOUTHWEST
        if (myPosition.x > nextPosition.x && nextPosition.y < myPosition.y) return WorkerAction.NORTHWEST

        if (myPosition.x < nextPosition.x && nextPosition.y > myPosition.y) return WorkerAction.SOUTHEAST
        if (myPosition.x < nextPosition.x && nextPosition.y < myPosition.y) return WorkerAction.NORTHEAST
        //failure
        return WorkerAction.TAKE
    }

    // React to action flags with logging
    private fun considerActionFlags(message: WorkerActionResponse, position: Position, vision: List<Position>) {
        when (message.flag) {
            ActionFlag.MAX_ACTIONS -> log.info(repairID + ": Max Action")
            ActionFlag.HAS_MATERIAL -> log.info(repairID + ": Has Material")
            ActionFlag.NO_ACTIVE_GAME -> log.info(repairID + ": No Active Game")
            ActionFlag.OBSTACLE -> doMove(position, vision)
            ActionFlag.NONE -> log.info(repairID + ": None")
            ActionFlag.NO_MATERIAL -> log.info(repairID + ": No Material")
        }
    }

    // Used for A* algorithm
    class Node(val position: Position, var gCost: Int, var hCost: Int, var parent: Node? = null) {
        val fCost: Int
            get() = gCost + hCost
    }
}
