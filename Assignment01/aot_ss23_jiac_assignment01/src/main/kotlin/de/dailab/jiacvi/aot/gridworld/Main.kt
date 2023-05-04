package de.dailab.jiacvi.aot.gridworld

import de.dailab.jiacvi.aot.gridworld.myAgents.EnvironmentAgent
import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem

fun main() {

    // you can create own grids and change the file here. Be sure to test with our grid as well.
    val gridfile = "/grids/benchmark.grid"

    agentSystem("Gridworld") {
        enable(LocalBroker)
        agents {
            // you can set logGames=true, logFile="logs/<name>.log" here
            add(ServerAgent(gridfile))

            // this is your Agent but don't change the ID
            add(EnvironmentAgent("env"))
        }
    }.start()
}
