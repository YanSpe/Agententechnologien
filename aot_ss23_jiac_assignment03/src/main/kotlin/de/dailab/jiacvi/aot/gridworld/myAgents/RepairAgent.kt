package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
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
    val repairPoints: List<Position> = repairPoints
    val size = size
    val repairID = repairID
    var CNPactive: Boolean = false
    lateinit var CNPmeetingPoint: Position


    override fun behaviour() = act {
        on<CurrentPosition> { message ->
            doTurn(message.position, message.vision)
        }

        // CNPmessage needs to be implemented
        respond<CNPrequest, CNPresponse> {message ->
            
            CNPmeetingPoint = message.meetingPoint
            CNPactive = true
        }
        on<TransferMaterial> {message ->
            hasMaterial = true
            CNPactive = false
        }
    }
    private fun doTurn(position: Position, vision: List<Position>) {
        log.info(repairID + vision)
        val ref = system.resolve(SERVER_NAME)
        if (standsOnRepairPoint && hasMaterial) {
            log.info(repairID + " takes material")
            ref invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, WorkerAction.DROP)) {
                if (it.state) {
                    hasMaterial = false
                    log.info(repairID + " has repaired a hole")
                } else {
                    considerActionFlags(it, position, vision)
                }
            }
        } else {
            doMove(position, vision)
        }
    }
    private fun doMove(position: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        var nextPosition: Position? = null
        if(!hasMaterial && CNPactive) {
            nextPosition = getNextPosition(position, CNPmeetingPoint)
        } else {
            var nearestRepairPoint: Position = getNearestRepairPoint(position)
            nextPosition = getNextPosition(position, nearestRepairPoint)
        }
        if (nextPosition != null) {
            var nextAction = getActionForPosition(position, nextPosition)

            ref invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, nextAction)) {
                if (!it.state) {
                    considerActionFlags(it, position, vision)
                }
            }
        }

    }

    private fun getNearestRepairPoint(position: Position): Position {
        //var nearestRepairPoint: Position = Position(x=-1, y=-1)
        //for (repairPoint in repairPoints){
        //    if (nearestRepairPoint == Position(x=-1, y=-1)){
        //        nearestRepairPoint = repairPoint
        //    } else if (repairPoint.x-position.x+repairPoint.y-position.y < nearestRepairPoint.x-position.x+nearestRepairPoint.y-position.y){
        //        nearestRepairPoint = repairPoint
        //    }
        //}
        return repairPoints.sortedBy { calculateDistance(position, it) }.first()
    }
    class Node(val position: Position, var gCost: Int, var hCost: Int, var parent: Node? = null) {
        val fCost: Int
            get() = gCost + hCost
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
    private fun getNextPosition(position: Position, goal: Position): Position?{
        val openList = mutableListOf<Node>()
        val closedList = mutableSetOf<Node>()

        val startNode = Node(position, 0, calculateDistance(position, goal))
        openList.add(startNode)

        while (openList.isNotEmpty()) {
            val currentNode = openList.minBy { it.fCost }
            openList.remove(currentNode)
            currentNode?.let {
                closedList.add(it)

                if (it.position == goal) {
                    // Reached the end position
                    return null
                }

                val adjacentPositions = getAdjacentPositions(it.position)
                for (adjacentPos in adjacentPositions) {
                    if (!isPositionValid(adjacentPos, obstacles) || closedList.any { node -> node.position == adjacentPos }) {
                        continue
                    }

                    val gCost = it.gCost + calculateDistance(it.position, adjacentPos)
                    val hCost = calculateDistance(adjacentPos, goal)
                    val newNode = Node(adjacentPos, gCost, hCost, it)

                    val existingNode = openList.find { node -> node.position == newNode.position }
                    if (existingNode == null || existingNode.fCost > newNode.fCost) {
                        existingNode?.let { node -> openList.remove(node) }
                        openList.add(newNode)
                    }
                }
            }
        }

        // No path found
        if (closedList.isEmpty()) {
            return null
        }

        // Get the best next position based on fCost
        val bestNode = closedList.minBy { it.fCost }
        return bestNode?.position
    }


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
}
