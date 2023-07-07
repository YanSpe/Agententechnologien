package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
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
    val knownMaterial: ArrayList<Position> = ArrayList()

    override fun behaviour() = act {
        on<CurrentPosition> { message ->
            doTurn(message.position, message.vision)
        }
    }

    private fun doTurn(position: Position, vision: List<Position>) {
        log.info(collectID + vision)
        val ref = system.resolve(SERVER_NAME)
        if (!hasMaterial && !vision.isEmpty() && !standsOnMaterial) {
            doMaterialMove(position, vision)
        } else if (standsOnMaterial && !hasMaterial) {
            log.info(collectID + " takes material")
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
            //CNP
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
            if(repairPoints.contains(position) ){
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

    private fun doMove(position: Position, vision: List<Position>) {
        val ref = system.resolve(SERVER_NAME)
        var possiblePositions = getPossiblePositions(position)
        var nextPosition = possiblePositions.get(Random.nextInt(0, possiblePositions.size - 1))
        var nextAction = getActionForPosition(position, nextPosition)

        ref invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, nextAction)) {
            if (!it.state) {
                considerActionFlags(it, position, vision)
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

}