package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * Stub for your AntAgent
 * */
class AntAgent(var antId: String) : Agent(overrideName = antId) {
    // TODO you might need to put some variables to save stuff here
    var position: Position = Position(0, 100)
    var nextPosition: Position = Position(0, 101)
    var nestPosition: Position = Position(0, 102)
    var lastPosition: Position = Position(100, 0)
    var holdingFood: Boolean = false
    var atFood: Boolean = false
    var amount: Double = 1.0
    var pos0: Position = Position(20, 0)
    var pos1: Position = Position(30, 0)
    var pos2: Position = Position(40, 0)
    var lastAction: AntAction = AntAction.NORTH
    var usePos1: Double = 0.70
    var usePos2: Double = 0.20

    fun getMovePos(possiblePositions: ArrayList<Position>): Position {
        val random = Random.nextDouble()
        if (random <= usePos1) return possiblePositions[0]
        if (random <= usePos1 + usePos2) return possiblePositions[1]
        return possiblePositions[2]
    }

    fun doAction() {
        val positionList: ArrayList<Position> = ArrayList()

        //log.info("Ich bin Ameise " + antId + " meine letzte Position war " + lastPosition + " und meine pos0 ist " + pos0)

        positionList.add(pos0)
        positionList.add(pos1)
        positionList.add(pos2)
        val move: Position = getMovePos(positionList)
        var action: AntAction? = null

        if (holdingFood && position == nestPosition) {
            //log.info("Ich bin Ameise " + antId + "und wähle DROP Position: " + position + " nestPosition: " + nestPosition)
            action = AntAction.DROP
        } else if (!holdingFood && atFood) {
            action = AntAction.TAKE
            //log.info("Ich bin Ameise " + antId + "und wähle Take Position: " + position + " atFood: " + atFood+ " holdingFood: "+ holdingFood)
        }
        if (action == null) {
            action = convertPositionToAction(position, move)
        }

        nextPosition = move

        lastAction = action

        //log.info("Ich bin Ameise " + antId + " und wähle action " + action)
        system.resolve("server") invoke ask<AntActionResponse>(AntActionRequest(antId, action)) { message ->

            //log.info("AntActionResponse für Ameise " + antId + ": " + message.state + " für Action "+ action +" at position: "+ position +", with flag: " + message.flag)
            if (message.state) {
                if (lastAction == AntAction.TAKE) {
                    holdingFood = true
                    amount = 1.0
                    atFood = false
                }

                if (lastAction == AntAction.DROP) {
                    holdingFood = false
                    amount = 1.0
                    atFood = false
                }

                system.resolve("env") tell PheromoneMessage(position, !holdingFood, amount)
                if (amount >= 0.07) {
                    amount -= 0.07
                }
                if (amount < 0.0){
                    amount = 0.0
                }

                if (lastAction != AntAction.TAKE && lastAction != AntAction.DROP) {
                    lastPosition = position
                    position = nextPosition
                }

                //log.info("New position for ant " + antId + ": " + position)
            }

            var a = 1
            when (message.flag) {
                ActionFlag.NO_ACTIVE_GAME -> system.terminate()//log.info("No Active Game")  // ant is not registered or no game started
                ActionFlag.MAX_ACTIONS -> a = 1//log.info("Ameise "+ antId + " tried too many actions")    // ants can only do 1 action per turn
                ActionFlag.OBSTACLE -> {
                    system.resolve("env") tell ObstacleMessage(move)
                }       // border of grid or obstacle (#) in grid
                ActionFlag.NO_FOOD -> {
                    if (lastAction == AntAction.TAKE){
                        atFood = false
                    }
                }        // ant has no food to drop or is not at active food source to take
                ActionFlag.NO_NEST -> a = 2//log.info("no nest")        // ant is not at nest while trying to drop
                ActionFlag.HAS_FOOD -> {
                    if (!holdingFood) {
                        atFood = true
                        //log.info("Ich bin Ameise " + antId + " und ich bin an einer Food Source and Position: "+ position)
                    }
                }   // new position is active food source or ant has food and can't take more
                ActionFlag.NONE -> a = 3//log.info("none")
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
        on { message: EnvironmentSetUpAntMessage ->
            position = message.position
            nestPosition = message.position
            //log.info("EnvironmentSetUpAntMessage: Ich bin Ameise " + antId + " und bin an Postion: " + position)
        }

        on { message: AntTurnMessage ->
            //log.info("AntturnInfo: " + message.turn)
            if (holdingFood) {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, true, antId, lastPosition)
            } else {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, false, antId, lastPosition)
            }
        }

        on { message: Return3BestPositionsMessage ->
            //log.info("Ich bin Ameise " + antId + " und habe folgende Koordinaten mit den meisten Pheromonen erhalten " + message.p0 + " " + message.p1 + " " + message.p2)
            pos0 = message.p0
            pos1 = message.p1
            pos2 = message.p2
            doAction()
        }

        on { message: EndGameMessage ->
            exitProcess(0)
        }

    }
}
