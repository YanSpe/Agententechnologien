package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*
import kotlin.collections.ArrayList

/**
 * Stub for your AntAgent
 * */
class AntAgent(antId: String): Agent(overrideName=antId) {
    // TODO you might need to put some variables to save stuff here
    var position: Position = Position(0,0)
    var nestPosition: Position = Position(0,0)
    var holdingFood: Boolean = false
    var antId: String = antId
    var atFood: Boolean = false
    var amount: Double = 1.0
    var pos0: Position = Position(0,0)
    var pos1: Position = Position(0,0)
    var pos2: Position = Position(0,0)
    var lastAction: AntAction = AntAction.NORTH

    fun doAction() {
        val positionList: ArrayList<Position> = ArrayList()
        positionList.add(pos0)
        positionList.add(pos1)
        positionList.add(pos2)
        val move: Position = positionList.shuffled().first()
        var action:AntAction

        if(holdingFood && position == nestPosition){
            lastAction = AntAction.DROP
            action = AntAction.DROP
            //system.resolve("server") tell AntActionRequest(antId, AntAction.DROP)
        }
        else if (!holdingFood && atFood){
            lastAction = AntAction.TAKE
            action = AntAction.TAKE
            //system.resolve("server") tell AntActionRequest(antId, AntAction.TAKE)
        }
        else{

            action = convertPositionToAction(position, move)
            lastAction = action


            log.info("Ich bin Ameise " + antId + " und wähle action " + action)
            //system.resolve("server") tell AntActionRequest(antId, action)
            system.resolve("server") invoke ask<AntActionResponse>(AntActionRequest(antId, action)){
                message ->

                log.info("AntActionResponse: " + message.state)
                if(message.state){
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

                }

                when (message.flag){
                    ActionFlag.NO_ACTIVE_GAME -> println("Error")  // ant is not registered or no game started
                    ActionFlag.MAX_ACTIONS -> println("too many actions")    // ants can only do 1 action per turn
                    ActionFlag.OBSTACLE -> doAction()       // border of grid or obstacle (#) in grid
                    ActionFlag.NO_FOOD -> println("no food")        // ant has no food to drop or is not at active food source to take
                    ActionFlag.NO_NEST -> println("no nest")        // ant is not at nest while trying to drop
                    ActionFlag.HAS_FOOD ->  {
                        if (!holdingFood) atFood = true
                    }   // new position is active food source or ant has food and can't take more
                    ActionFlag.NONE -> println("none")
                }

            }
        }
    }

    fun convertPositionToAction(p0: Position, p1: Position): AntAction{
        if(p0.x==p1.x && p0.y<p1.y) {
            return AntAction.NORTH
        }
        if(p0.x<p1.x && p0.y<p1.y) {
            return AntAction.NORTHEAST
        }
        if(p0.x<p1.x && p0.y==p1.y) {
            return AntAction.EAST
        }
        if(p0.x<p1.x && p0.y > p1.y) {
            return AntAction.SOUTHEAST
        }
        if(p0.x==p1.x && p0.y>p1.y) {
            return AntAction.SOUTH
        }
        if(p0.x>p1.x && p0.y>p1.y) {
            return AntAction.SOUTHWEST
        }
        if(p0.x>p1.x && p0.y==p1.y) {
            return AntAction.WEST
        }
        else {
            return AntAction.NORTHWEST
        }
    }
    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to make a move in the gridworld
        *   - build your ant algorithm by communicating with your environment when looking for the way
        *   - adjust your parameters to get better results
        */

        on {    message: EnvironmentSetUpAntMessage ->
            position = message.position
            nestPosition = message.position
            log.info("EnvironmentSetUpAntMessage: Ich bin Ameise "+ antId + " und bin an Postion: " + position)
        }

        on { message: AntTurnInformation ->
            //log.info("AntturnInfo: " + message.turn)
            if (holdingFood) {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, false, antId)
            } else {
                system.resolve("env") tell InspectPheromoneEnvironmentMessage(position, true, antId)
            }
        }

        on { message: ReturnPheromoneEnvironmentMessage ->
            log.info("Ich bin Ameise "+ antId+ " und habe folgende Koordinaten mit den meisten Pheromonen erhalten " + message.p0 + " " + message.p1+ " " + message.p2)
            pos0 = message.p0
            pos1 = message.p1
            pos2 = message.p2
            doAction()
        }

        on {message: EndGameMessage ->
            system.terminate()
        }

        /*
        on { message: AntActionResponse ->
            log.info("AntActionResponse: " + message.state)
            if(message.state){
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

            }

            when (message.flag){
                ActionFlag.NO_ACTIVE_GAME -> println("Error")  // ant is not registered or no game started
                ActionFlag.MAX_ACTIONS -> println("too many actions")    // ants can only do 1 action per turn
                ActionFlag.OBSTACLE -> doAction()       // border of grid or obstacle (#) in grid
                ActionFlag.NO_FOOD -> println("no food")        // ant has no food to drop or is not at active food source to take
                ActionFlag.NO_NEST -> println("no nest")        // ant is not at nest while trying to drop
                ActionFlag.HAS_FOOD ->  {
                    if (!holdingFood) atFood = true
                }   // new position is active food source or ant has food and can't take more
                ActionFlag.NONE -> println("none")
            }

        }

         */
    }
}
