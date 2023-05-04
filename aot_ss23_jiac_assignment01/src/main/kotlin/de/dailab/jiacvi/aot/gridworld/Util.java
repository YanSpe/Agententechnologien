package de.dailab.jiacvi.aot.gridworld;

import java.io.InputStream;
import java.util.*;



/**
 * Helper class to read gridfiles.
 */
public class Util {

	public static final Random random = new Random();



	/**
	 * Load gridworld game from the given file, to be found on the class path,
	 * more specifically in the resources/grids directory; the filename must
	 * be absolute (relative to the class-path root) and should include the
	 * root "/", e.g. "/grid/somegridfile.grid".
	 */
	public static GridworldGame loadGameFromFile(String filename) {
		InputStream is = Util.class.getResourceAsStream(filename);
		if (is == null) {
			throw new IllegalArgumentException("Invalid grid file: " + filename);
		}
		try (Scanner scanner = new Scanner(is)) {

			// first line: general game parameters
			int width = scanner.nextInt();
			int height = scanner.nextInt();
			int turns = scanner.nextInt();
			int numFoodSources = scanner.nextInt();
			int nestX = scanner.nextInt();
			int nestY = scanner.nextInt();

			final GridworldGame game = new GridworldGame(
					new Position(width, height), -1, turns,
					new Nest(new Position(nestX, nestY), new HashSet<>(), 0), new HashMap<>(), new HashSet<>(), "logs/game-"+random.nextInt(Integer.MAX_VALUE)+".log"
			);

			// next height lines: grid and obstacles
			for (int y = 0; y < height; y++) {
				String line = scanner.next(String.format(".{%d}", width));
				for (int x = 0; x < width; x ++) {
					if (line.charAt(x) == '#') {
						game.getObstacles().add(new Position(x, y));
					}
				}
			}

			// next numFoodSources lines: the orders
			for (int f = 0; f < numFoodSources; f++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();
				int created  = scanner.nextInt();
				int value    = scanner.nextInt();

				Food food = new Food(id, new Position(x, y), value, created, value);
				game.getFoodSources().put(food.getId(), food);
			}



			return game;
		}
	}

}
