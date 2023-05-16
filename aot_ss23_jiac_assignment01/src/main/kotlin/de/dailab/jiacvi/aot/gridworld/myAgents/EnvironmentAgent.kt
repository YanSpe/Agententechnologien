package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import kotlin.random.Random
import kotlin.system.exitProcess


/**
 * Stub for your EnvironmentAgent
 * */
class EnvironmentAgent(private val envId: String) : Agent(overrideName = envId) {
    private val numberOfAnts: Int = 40
    private val antAgentsId: ArrayList<String> = ArrayList()

    private var size: Position = Position(1, 1)
    private var nestPosition: Position = Position(1, 1)
    private var obstacles: ArrayList<Position>? = null

    var nestPheromones: Array<Array<Double>> = Array(1) { Array(1) { 0.0 } }

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
        listen(BROADCAST_TOPIC) { message: GameTurnInform ->
            //log.info("GameTurnInfo-Env: " + message.gameTurn)
            updatePheromones(foodPheromones, 0.06)
            updatePheromones(nestPheromones, 0.03)
            //log.info("Foodpheromones: " + printPheromones(foodPheromones))

            for (ant in antAgentsId) {
                system.resolve(ant) tell AntTurnMessage(message.gameTurn)
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
            val possiblePos: ArrayList<Position> =
                get3BestPositions(message.position, message.useNestPheromone, message.lastPosition)

            system.resolve(message.antID) tell Return3BestPositionsMessage(
                possiblePos.get(0),
                possiblePos.get(1),
                possiblePos.get(2)
            )
        }

        on { message: ObstacleMessage ->
            obstaclesFound[message.obstaclePosition.x][message.obstaclePosition.y] = 1.0
            //log.info("Obstacle found at: " + message.obstaclePosition +" Alle obstacles: " + printPheromones(obstaclesFound))
        }

        on { message: EndGameMessage ->
            log.info("score: " + message.score)
            exitProcess(0)
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
                if (a[i][j] < 0.0) {
                    a[i][j] = 0.0
                }
            }
        }
        return
    }

    private fun printPheromones(a: Array<Array<Double>>): String {
        var retString = ""
        for (row in a) {
            retString += "," + row.contentToString()
        }
        return retString
    }

    private fun get3BestPositions(
        antPosition: Position,
        useNestPheromone: Boolean,
        lastPosition: Position
    ): ArrayList<Position> {
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
            }
        }
        //log.info("print sortPosList: " + printValueList(sortPosList))
        var sortedList = sortPosList.sortedBy { sortPos -> sortPos.value }
        //log.info("print sortedList: " + printValueList(sortedList))
        var allZero = true
        for (sorted: SortPos in sortedList) {
            if (sorted.value != 0.0) allZero = false
        }
        val x: ArrayList<Position> = ArrayList()

        if (allZero) {
            sortedList = sortedList.shuffled()
        }
        //log.info("print sortedList: " + printValueList(sortedList))
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
            for (element in sortedList) {
                x.add(element.position)
            }
        }

        var iteration1 = 0
        while (x.size < 3 && iteration1 < 5) {

            //log.info("Warning: x.size == 0")
            val xrand = (-1..1).random() + antPosition.x
            val yrand = (-1..1).random() + antPosition.y
            if (xrand < size.x && xrand >= 0 && yrand < size.y && yrand >= 0) {
                if (obstaclesFound[xrand][yrand] != 1.0) {
                    x.add(Position(xrand, yrand))
                }
            }

            iteration1++
        }

        // Logik der Ant ins Environment

        if (x[0] == lastPosition && !useNestPheromone) {
            //val random = 0
            var iteration = 0
            while ((x[0] == lastPosition || x[0] == antPosition) && iteration < 5) {
                val random = Random.nextDouble()
                val xnew = antPosition.x - x[0].x + antPosition.x
                val ynew = antPosition.y - x[0].y + antPosition.y
                if (random <= 0.5 && xnew < size.x && xnew >= 0 && ynew < size.y && ynew >= 0) {
                    if (obstaclesFound[xnew][ynew] != 1.0) {
                        x[0] = Position(xnew, ynew)
                    }
                } else {
                    val xrand = (-1..1).random() + antPosition.x
                    val yrand = (-1..1).random() + antPosition.y
                    if (xrand < size.x && xrand >= 0 && yrand < size.y && yrand >= 0) {
                        if (obstaclesFound[xrand][yrand] != 1.0) {
                            x[0] = Position(xrand, yrand)
                        }
                    }
                }
                iteration++
                //log.info("x[0] was changed to: "+ x[0]+ " from: "+ lastPosition)
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

