package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import kotlin.random.Random


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String) : Agent(overrideName = envId) {
    // TODO you might need to put some variables to save stuff here

    private val numberOfAnts: Int = 40
    private val antAgentsId: ArrayList<String> = ArrayList()

    private var size: Position = Position(1, 1)
    private var nestPosition: Position = Position(1, 1)
    private var obstacles: ArrayList<Position>? = null

    //private val xNest: IntArray = intArrayOf(size.x)
    //private val yNest: IntArray = intArrayOf(size.y)
    //private val nestPheromones: Array<IntArray> = arrayOf(xNest, yNest)
    var nestPheromones: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }

    //private val xFood: IntArray = intArrayOf(size.x)
    //private val yFood: IntArray = intArrayOf(size.y)
    //private var foodPheromones: Array<IntArray> = arrayOf(xFood, yFood)
    var foodPheromones: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }
    var obstaclesFound: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }


    override fun preStart() {
        super.preStart()
        system.resolve("server") invoke ask<StartGameResponse>(StartGameMessage(envId, intialize())) { message ->
            size = message.size
            nestPosition = message.nestPosition
            obstacles = message.obstacles as ArrayList<Position>?

            nestPheromones = Array(size.x) { Array(size.y) { 0.0 } }
            foodPheromones = Array(size.x) { Array(size.y) { 0.0 } }
            obstaclesFound = Array(size.x) { Array(size.y) { 0.0 } }

            for (ant in antAgentsId) {
                system.resolve(ant) tell EnvironmentSetUpAntMessage(nestPosition)
            }
        }.error {
            println("error")
        }

    }

    override fun behaviour() = act {
        /* TODO here belongs most of your agents logic.
        *   - Check the readme "Reactive Behaviour" part and see the Server for some examples
        *   - try to start a game with the StartGameMessage
        *   - you need to initialize your ants, they don't know where they start
        *   - here you should manage the pheromones dropped by your ants
        *   - REMEMBER: pheromones should transpire, so old routes get lost
        *   - adjust your parameters to get better results, i.e. amount of ants (capped at 40)
        */
        listen(BROADCAST_TOPIC) { message: GameTurnInform ->
            //log.info("GameTurnInfo-Env: " + message.gameTurn)
            updatePheromones(foodPheromones, 0.06)
            updatePheromones(nestPheromones, 0.03)
            //log.info("Foodpheromones: " + printPheromones(foodPheromones))

            for (ant in antAgentsId) {
                system.resolve(ant) tell AntTurnInformation(message.gameTurn)
            }

        }
        on { message: PheromoneMessage ->
            //log.info("Updated useNestPheromone: " + message.useNestPheromone + " at position " + message.position)
            if (message.useNestPheromone) {
                nestPheromones[message.position.x][message.position.y] += message.amount
            } else {
                foodPheromones[message.position.x][message.position.y] += message.amount
            }
            //log.info("Nestpheromones: " + printPheromones(nestPheromones))
            //log.info("Foodpheromones: " + printPheromones(foodPheromones))
        }


        on { message: InspectPheromoneEnvironmentMessage ->
            //log.info("Ameise " + message.antID + " fragt nach Pheromonen an Stelle " + message.position + " mit useNestPheromon: " + message.useNestPheromone)
            val possiblePos: ArrayList<Position> = getPossiblePositions(message.position, message.useNestPheromone, message.lastPosition)

            system.resolve(message.antID) tell ReturnPheromoneEnvironmentMessage(
                possiblePos.get(0),
                possiblePos.get(1),
                possiblePos.get(2),
                obstaclesFound
            )
        }

        on { message: ObstacleMessage ->
            obstaclesFound[message.obstaclePosition.x][message.obstaclePosition.y] = 1.0
            log.info("Obstacle found at: " + message.obstaclePosition +" Alle obstacles: " + printPheromones(obstaclesFound))
        }

        on { message: EndGameMessage ->
            system.terminate()
        }

    }

    private fun intialize(): List<String> {
        var antNumber = 0
        while (antNumber < numberOfAnts) {
            system.spawnAgent(AntAgent(antNumber.toString()), null, antNumber.toString())
            antAgentsId.add(antNumber.toString())
            antNumber++
        }
        return antAgentsId
    }

    private fun updatePheromones(a: Array<Array<Double>>, x: Double) {
        for (i in 0 until a.size) {
            for (j in 0 until a[i].size) {
                if (a[i][j] != 0.0) {
                    a[i][j] -= x
                }
                if (a[i][j] < 0.0){
                    a[i][j] = 0.0
                }

            }
        }
        return
    }

    private fun printPheromones(a: Array<Array<Double>>): String {
        var retString: String = ""
        for (row in a) {
            retString += "," + row.contentToString()
        }
        return retString
    }

    private fun getPossiblePositions(antPosition: Position, useNestPheromone: Boolean, lastPosition: Position): ArrayList<Position> {
        val positionList: ArrayList<Position> = ArrayList()

        if (antPosition.x + 1 < size.x) {
            positionList.add(Position(antPosition.x + 1, antPosition.y))
            if (antPosition.y + 1 < size.y) positionList.add(Position(antPosition.x + 1, antPosition.y + 1))
            if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x + 1, antPosition.y - 1))
        }
        if (antPosition.x - 1 >= 0) {
            positionList.add(Position(antPosition.x - 1, antPosition.y))
            if (antPosition.y + 1 < size.y) positionList.add(Position(antPosition.x - 1, antPosition.y + 1))
            if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x - 1, antPosition.y - 1))
        }

        if (antPosition.y + 1 < size.y) positionList.add(Position(antPosition.x, antPosition.y + 1))
        if (antPosition.y - 1 >= 0) positionList.add(Position(antPosition.x, antPosition.y - 1))

        val sortPosList: ArrayList<SortPos> = ArrayList()
        for (positionToSort: Position in positionList) {
           if (obstaclesFound[positionToSort.x][positionToSort.y] != 1.0) {
               sortPosList.add(SortPos(positionToSort, getMapValForPosition(positionToSort, useNestPheromone)))
           } else {
               //log.info("Removed position "+ positionToSort + " from considered positions due to obstacle")
           }
        }
        //log.info("print sortPosList: " + printValueList(sortPosList))
        var sortedList = sortPosList.sortedBy { sortPos -> sortPos.value }
        //log.info("print sortedList: " + printValueList(sortedList))
        var allZero = true
        for (sorted: SortPos in sortedList) {
            if (sorted.value != 0.0) allZero = false
        }
        if (allZero) {
            sortedList = sortedList.shuffled()
        }
        //log.info("print sortedList: " + printValueList(sortedList))
        val x: ArrayList<Position> = ArrayList()
        if (sortedList.size >= 3) {
            //log.info("The Best Position is " + sortedList[sortedList.size - 1].position + " with the value " + sortedList[sortedList.size - 1].value)

            x.add(sortedList[sortedList.size - 1].position)
            if (sortedList[sortedList.size - 2].value == 0.0 && sortedList[sortedList.size - 1].value != 0.0) {
                x.add(sortedList[sortedList.size - 1].position)
            } else {
                x.add(sortedList[sortedList.size - 2].position)
            }
            x.add(sortedList[sortedList.size - 3].position)
        } else {
            // bei 0 möglichen Positionen schmiert das ganze ab --> weiß aber auch nicht, wie es dazu kommen sollte
            val x: ArrayList<Position> = ArrayList()
            for (element in sortedList) {
                x.add(element.position)
            }
            for (i in 1..3) {
                if (x.size == 3) break
                else {
                    x.add(x.get(x.lastIndex - 1))
                }
            }
        }
        // Logik der Ant ins Environment
        if (x[0] == lastPosition && !useNestPheromone){
            //val random = 0
                while (x[0] == lastPosition || x[0] == antPosition){
                    val random = Random.nextDouble()
                    var xnew = antPosition.x-x[0].x+antPosition.x
                    var ynew = antPosition.y-x[0].y+antPosition.y
                    if(random <= 0.5 && xnew < size.x && xnew >= 0 && ynew < size.y && ynew >= 0){
                        if (obstaclesFound[xnew][ynew] != 1.0){
                            x[0] = Position(xnew, ynew)
                        }
                    } else {
                        var xrand = (-1..1).random()+antPosition.x
                        var yrand = (-1..1).random()+antPosition.y
                        if(xrand < size.x && xrand >= 0 && yrand < size.y && yrand >= 0){
                            if (obstaclesFound[xrand][yrand] != 1.0){
                                x[0] = Position(xrand, yrand)
                            }
                        }
                    }
                    log.info("x[0] was changed to: "+ x[0]+ " from: "+ lastPosition)
            }

            //log.info("Ich bin Ameise " + antId + " mit random = " + random + " und neuer p0: " + pos0)
        }

        return x
    }

    private fun getMapValForPosition(position: Position, useNestPheromone: Boolean): Double {
        var map: Array<Array<Double>> = nestPheromones
        if (!useNestPheromone) map = foodPheromones
        return map[position.x][position.y]
    }

    private fun printValueList(list: List<SortPos>): String {
        var s = ""
        for (elem in list) {
            s += ", " + elem.value
        }
        return s
    }
}

data class SortPos(
    val position: Position,
    val value: Double
)

