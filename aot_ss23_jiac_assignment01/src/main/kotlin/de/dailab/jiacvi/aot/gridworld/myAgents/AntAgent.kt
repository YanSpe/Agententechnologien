package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import kotlin.collections.ArrayList

/**
 * Stub for your AntAgent
 * */
class AntAgent(antId: String) : Agent(overrideName = antId) {
    // TODO you might need to put some variables to save stuff here
    var position: Position = Position(0, 0)
    var nextPosition: Position = Position(0, 0)
    var nestPosition: Position = Position(0, 0)
    var holdingFood: Boolean = false
    var antId: String = antId
    var atFood: Boolean = false
    var amount: Double = 1.0
    var pos0: Position = Position(0, 0)
    var pos1: Position = Position(0, 0)
    var pos2: Position = Position(0, 0)
    var lastAction: AntAction = AntAction.NORTH

    fun doAction() {
        val positionList: ArrayList<Position> = ArrayList()
        positionList.add(pos0)
        positionList.add(pos1)
        positionList.add(pos2)
        val move: Position = positionList.shuffled().first()
        var action: AntAction? = null

        if (holdingFood && position == nestPosition) {
            log.info("Ich bin Ameise " + antId + "und wähle DROP Position: " + position + " nestPosition: " + nestPosition)
            action = AntAction.DROP
        } else if (!holdingFood && atFood) {
            action = AntAction.TAKE
        }
        if (action == null) {
            action = convertPositionToAction(position, move)
        }

        nextPosition = move

        lastAction = action

        log.info("Ich bin Ameise " + antId + " und wähle action " + action)
        //system.resolve("server") tell AntActionRequest(antId, action)
        system.resolve("server") invoke ask<AntActionResponse>(AntActionRequest(antId, action)) { message ->

            log.info("AntActionResponse für Ameise " + antId + ": " + message.state)
            if (message.state) {
                if (lastAction == AntAction.TAKE) {
                    holdingFood = true
                    amount = 1.0
                }

                if (lastAction == AntAction.DROP) {
                    holdingFood = false
                    amount = 1.0
                }

                system.resolve("env") tell PheromoneMessage(position, !holdingFood, amount)
                if (amount >= 0.05) {
                    amount -= 0.05
                }
                position = nextPosition
                log.info("New position for ant " + antId + ": " + position)
            } else if (message.flag != ActionFlag.MAX_ACTIONS && message.flag != ActionFlag.NO_ACTIVE_GAME) {
                //doAction()

            }

            when (message.flag) {
                ActionFlag.NO_ACTIVE_GAME -> log.info("No Active Game")  // ant is not registered or no game started
                ActionFlag.MAX_ACTIONS -> log.info("too many actions")    // ants can only do 1 action per turn
                ActionFlag.OBSTACLE -> doAction()       // border of grid or obstacle (#) in grid
                ActionFlag.NO_FOOD -> doAction()        // ant has no food to drop or is not at active food source to take
                ActionFlag.NO_NEST -> doAction()        // ant is not at nest while trying to drop
                ActionFlag.HAS_FOOD -> {
                    if (!holdingFood) {
                        atFood = true
                        log.info("Ich bin Ameise " + antId + " und ich bin an einer Food Source")
                    }
                }   // new position is active food source or ant has food and can't take more
                ActionFlag.NONE -> log.info("none")
            }

        }

    }

    fun convertPositionToAction(thisPosition: Position, positionToSet: Position): AntAction {
        if (thisPosition.x == positionToSet.x && thisPosition.y < positionToSet.y) {
            return AntAction.SOUTH
        }
        if (thisPosition.x < positionToSet.x && thisPosition.y < positionToSet.y) {
            return AntAction.SOUTHEAST
        }
        if (thisPosition.x < positionToSet.x && thisPosition.y == positionToSet.y) {
            return AntAction.EAST
        }
        if (thisPosition.x < positionToSet.x && thisPosition.y > positionToSet.y) {
            return AntAction.NORTHEAST
        }
        if (thisPosition.x == positionToSet.x && thisPosition.y > positionToSet.y) {
            return AntAction.NORTH
        }
        if (thisPosition.x > positionToSet.x && thisPosition.y > positionToSet.y) {
            return AntAction.NORTHWEST
        }
        if (thisPosition.x > positionToSet.x && thisPosition.y == positionToSet.y) {
            return AntAction.WEST
        } else {
            return AntAction.SOUTHWEST
        }
    }

    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to make a move in the gridworld
        *   - build your ant algorithm by communicating with your environment when looking for the way
        *   - adjust your parameters to get better results
        */

        on { message: EnvironmentSetUpAntMessage ->
            position = message.position
            nestPosition = message.position
            log.info("EnvironmentSetUpAntMessage: Ich bin Ameise " + antId + " und bin an Postion: " + position)
        }

        on { message: AntTurnInformation ->
            //log.info("AntturnInfo: " + message.turn)
            if (holdingFood) {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, true, antId)
            } else {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, false, antId)
            }
        }

        on { message: ReturnPheromoneEnvironmentMessage ->
            log.info("Ich bin Ameise " + antId + " und habe folgende Koordinaten mit den meisten Pheromonen erhalten " + message.p0 + " " + message.p1 + " " + message.p2)
            pos0 = message.p0
            pos1 = message.p1
            pos2 = message.p2
            doAction()
        }

        on { message: EndGameMessage ->
            system.terminate()
        }

    }
}
