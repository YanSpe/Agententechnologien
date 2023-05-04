Example levels for Gridworld Game
=================================

This folder contains some example levels for the Gridworld game.


File Format
-----------

Each Grid file has to be formatted as follows:

* the first line consists of 6 integers separated by spaces or tabs: `width`, `height`, `turns`, `num_food_sources`, `nest_x`, `nest_y` 
* the next `height` lines consist of `width` characters describing the respective rows of the grid; here, a `#` marks an obstacle, and a `.` marks a free space; other characters can be used to indicate the positions of food or ants, but will be _ignored_ here; only the obstacles are considered
* next `num_food_sources` lines each have a string `food_id` and four integers `x`, `y`, `turn_available`, `food_amount`

All lines after that are ignored and can be used e.g. for documentation.

Example
-------

    10 10 20 2 1 1
    ..................f.
    .N..................
    ....................
    ....................
    ....................
    .........##.........
    .........##.........
    ....................
    ....................
    .f..................
    f1  1 9  0 1
    f2  8 0  0 2

