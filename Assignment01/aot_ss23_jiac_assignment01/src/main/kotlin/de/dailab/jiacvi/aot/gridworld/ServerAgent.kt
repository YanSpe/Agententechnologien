package de.dailab.jiacvi.aot.gridworld

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import java.time.Duration

const val SERVER_NAME = "server"
const val BROADCAST_TOPIC = "broadcast"


class ServerAgent(
		private val gridFile: String,
		private val maxAnts: Int = 40,
		private val revealObstacles: Boolean = false,
		private val logGames: Boolean = false,
		private val logFile: String = ""
): Agent(overrideName = SERVER_NAME) {

	// msgBroker only used for sending broadcasts
	private val msgBroker by resolve<BrokerAgentRef>()
	private var activeGame: GridworldGame? = null

	override fun behaviour() = act {

		// start or restart a new game
		respond<StartGameMessage, StartGameResponse> { msg ->
			log.info("Received $msg")
			if (activeGame != null) log.warn("Restarting game with game still running.")

			// read gridfile, see main. start with turn=-1 so that after increment in execute the first turn is 0
			val game = Util.loadGameFromFile(gridFile)

			// set up logging if active
			if (logFile.isNotEmpty()) game.logfile=logFile

			// register antIDs for this game, additional ants above the limit will simply be ignored
			for (a in msg.antIDs) {
				if (game.nest.ants.size == maxAnts) break
				val ant = Ant(a, game.nest.position, 0, false)
				game.nest.ants.add(ant)
				system.resolve(a) tell "welcome little ant"
			}


			activeGame = game

			// send response
			StartGameResponse(game.size, game.nest.position,
					if (revealObstacles) game.obstacles.toList() else null
			)
		}

		// manage active game
		every(Duration.ofMillis(200)) {
			if (activeGame != null) {
				val game = activeGame!!
				game.turn++
				if (game.turn > game.maxTurns) {
					finishGame(game)
				}
				// broadcast current turn
				msgBroker.publish(BROADCAST_TOPIC, GameTurnInform(game.turn))

				// logging if active
				if (logGames) game.log()

				// print grid to debug
				println("\n${game.prettyPrint()}\n")
			}
		}


		// handle incoming requests from ants, check model and message files for details
		respond<AntActionRequest, AntActionResponse> { msg ->
			log.info("Received $msg")
			if (!hasActiveGame(msg.antId)) { return@respond AntActionResponse(false, ActionFlag.NO_ACTIVE_GAME) }
			if (!allowedToAct(msg.antId)) { return@respond AntActionResponse(false, ActionFlag.MAX_ACTIONS) }

			val game = activeGame!!
			val ant = game.nest.ants.find { it.id == msg.antId }!!

			ant.lastTurn = game.turn
			when (msg.action) {
				AntAction.TAKE -> tryTake(ant)
				AntAction.DROP -> tryDrop(ant)
				else -> tryMove(ant, msg.action)
			}
		}

	}

	/** check if ant move is possible and whether the new field holds food */
	private fun tryMove(ant: Ant, move: AntAction): AntActionResponse {
		if (!movePossible(ant, move)) { return AntActionResponse(false, ActionFlag.OBSTACLE) }

		ant.position = ant.position.applyMove(move,  activeGame!!.size)!!
		if (activeGame!!.foodSources.filterValues { fs -> fs.position == ant.position && fs.available > 0}.isNotEmpty()) {
			return AntActionResponse(true, ActionFlag.HAS_FOOD)
		}

		return AntActionResponse(true, ActionFlag.NO_FOOD)
	}

	/** check if ant is at food and has space to take it */
	private fun tryTake(ant: Ant): AntActionResponse {
		val food = activeGame!!.foodSources.filterValues { fs -> fs.position == ant.position && fs.available > 0}
		if (ant.hasFood) { return AntActionResponse(false, ActionFlag.HAS_FOOD) }
		if (food.isEmpty()) { return AntActionResponse(false, ActionFlag.NO_FOOD) }


		val foodID = food.keys.first()
		activeGame!!.foodSources[foodID]!!.available--
		ant.hasFood = true

		return AntActionResponse(true)
	}

	/** check if ant is at nest position and has food to drop */
	private fun tryDrop(ant: Ant): AntActionResponse {
		if (!ant.hasFood) { return AntActionResponse(false, ActionFlag.NO_FOOD) }
		if (ant.position != activeGame!!.nest.position) { return AntActionResponse(false, ActionFlag.NO_NEST) }

		activeGame!!.nest.food++
		ant.hasFood = false

		return AntActionResponse(true)
	}

	/*
	* ---- collection of guard functions ----
	*/
	private fun hasActiveGame(id: String): Boolean {
		if (activeGame == null) return false
		return activeGame!!.nest.ants.find { it.id == id } != null
	}

	private fun allowedToAct(id: String): Boolean {
		return activeGame!!.nest.ants.find { it.id == id }!!.lastTurn < activeGame!!.turn
	}

	private fun movePossible(ant: Ant, move: AntAction): Boolean {
		val newPos = ant.position.applyMove(move,  activeGame!!.size)
		return (newPos != null && newPos !in  activeGame!!.obstacles)
	}


	/** shutdown currently running game */
	private fun finishGame(game: GridworldGame) {
		log.info("Finishing game...")
		val foodSum = game.foodSources.values.sumBy { it.value }
		system.resolve("env") tell  EndGameMessage(game.nest.food, foodSum, 100.0*game.nest.food/foodSum)
		activeGame = null
	}
}
