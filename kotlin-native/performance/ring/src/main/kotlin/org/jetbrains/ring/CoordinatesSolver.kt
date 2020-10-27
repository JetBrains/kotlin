package org.jetbrains.ring

import kotlin.experimental.and

class CoordinatesSolverBenchmark {
    val solver: Solver

    init {
        val inputValue = """
            12 5 3 25 3 9 3 9 1 3
            13 3 12 6 10 10 12 2 10 10
            9 2 9 5 6 12 5 0 2 10
            10 14 12 5 3 9 5 2 10 10
            8 1 3 9 4 0 3 14 10 10
            12 0 4 6 9 6 12 5 6 10
            11 12 3 9 6 9 5 3 9 6
            8 5 6 8 3 12 7 10 10 11
            12 3 13 6 12 3 9 6 12 2
            13 4 5 5 5 6 12 5 5 2
            1""".trimIndent()
        val input = readTillParsed(inputValue)

        solver = Solver(input!!)
    }

    data class Coordinate(val x: Int, val y: Int)

    @SinceKotlin("1.1")
    data class Field(val x: Int, val y: Int, val value: Byte) {
        fun northWall(): Boolean {
            return value and 1 != 0.toByte()
        }

        fun eastWall(): Boolean {
            return value and 2 != 0.toByte()
        }

        fun southWall(): Boolean {
            return value and 4 != 0.toByte()
        }

        fun westWall(): Boolean {
            return value and 8 != 0.toByte()
        }

        fun hasObject(): Boolean {
            return value and 16 != 0.toByte()
        }
    }

    class Input(val labyrinth: Labyrinth, val nObjects: Int)

    class Labyrinth(val width: Int, val height: Int, val fields: Array<Field>) {
        fun getField(x: Int, y: Int): Field {
            return fields[x + y * width]
        }
    }

    class Output(val steps: List<Coordinate?>)

    class InputParser {
        private val rows : MutableList<Array<Field>> = mutableListOf()
        private var numObjects: Int = 0

        private val input: Input
            get() {
                val width = rows[0].size
                val fields = arrayOfNulls<Field>(width * rows.size)

                for (y in rows.indices) {
                    val row = rows[y]
                    for (p in y*width until y*width + width) {
                        fields[p] = row[p-y*width]
                    }
                }

                val labyrinth = Labyrinth(width, rows.size, fields.requireNoNulls())

                return Input(labyrinth, numObjects)
            }

        fun feedLine(line: String): Input? {
            val items = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (items.size == 1) {
                numObjects = items[0].toInt()

                return input
            } else if (items.size > 0) {
                val rowNum = rows.size
                val row = arrayOfNulls<Field>(items.size)

                for (col in items.indices) {
                    row[col] = Field(rowNum, col, items[col].toByte())
                }

                rows.add(row.requireNoNulls())
            }

            return null
        }
    }


    class Solver(private val input: Input) {
        private val objects: List<Coordinate>

        private val width: Int
        private val height: Int
        private val maze_end: Coordinate

        private var counter: Long = 0

        init {

            objects = ArrayList()
            for (f in input.labyrinth.fields) {
                if (f.hasObject()) {
                    objects.add(Coordinate(f.x, f.y))
                }
            }

            width = input.labyrinth.width
            height = input.labyrinth.height
            maze_end = Coordinate(width - 1, height - 1)
        }

        fun solve(): Output {
            val steps = ArrayList<Coordinate>()

            for (o in objects.indices) {
                var limit = input.labyrinth.width + input.labyrinth.height - 2

                var ss: List<Coordinate>? = null
                while (ss == null) {
                    if (o == 0) {
                        ss = solveWithLimit(limit, MAZE_START) { it[it.size - 1] == objects[0] }
                    } else {
                        ss = solveWithLimit(limit, objects[o - 1]) { it[it.size - 1] == objects[o] }
                    }

                    if (ss != null) {
                        steps.addAll(ss)
                    }

                    limit++
                }
            }

            var limit = input.labyrinth.width + input.labyrinth.height - 2

            var ss: List<Coordinate>? = null
            while (ss == null) {
                ss = solveWithLimit(limit, objects[objects.size - 1]) { it[it.size - 1] == maze_end }

                if (ss != null) {
                    steps.addAll(ss)
                }

                limit++
            }

            return createOutput(steps)
        }

        private fun createOutput(steps: List<Coordinate>): Output {
            val objects : MutableList<Coordinate> = this.objects.toMutableList()
            val outSteps : MutableList<Coordinate?> = mutableListOf()

            for (step in steps) {
                outSteps.add(step)

                if (objects.contains(step)) {
                    outSteps.add(null)
                    objects.remove(step)
                }
            }

            return Output(outSteps)
        }

        private fun isValid(steps: List<Coordinate>): Boolean {
            counter++
            val (x, y) = steps[steps.size - 1]
            return if (!(x == input.labyrinth.width - 1 && y == input.labyrinth.height - 1)) { // Jobb also a cel
                false
            } else steps.containsAll(objects)

        }

        private fun getPossibleSteps(now: Coordinate, previous: Coordinate?): ArrayList<Coordinate> {
            val field = input.labyrinth.getField(now.x, now.y)

            val possibleSteps = ArrayList<Coordinate>()

            if (now.x != width - 1 && !field.eastWall()) {
                possibleSteps.add(Coordinate(now.x + 1, now.y))
            }
            if (now.x != 0 && !field.westWall()) {
                possibleSteps.add(Coordinate(now.x - 1, now.y))
            }
            if (now.y != 0 && !field.northWall()) {
                possibleSteps.add(Coordinate(now.x, now.y - 1))
            }
            if (now.y != height - 1 && !field.southWall()) {
                possibleSteps.add(Coordinate(now.x, now.y + 1))
            }

            if (!field.hasObject() && previous != null) {
                possibleSteps.remove(previous)
            }

            return possibleSteps
        }

        private fun solveWithLimit(limit: Int, start: Coordinate, validFn: (List<Coordinate>) -> Boolean): List<Coordinate>? {
            var steps: MutableList<Coordinate>? = findFirstLegitSteps(null, start, limit)

            while (steps != null && !validFn(steps)) {
                steps = alter(start, null, steps)
            }

            return steps
        }

        private fun findFirstLegitSteps(startPrev: Coordinate?, start: Coordinate, num: Int): MutableList<Coordinate>? {
            var steps: MutableList<Coordinate>? = ArrayList()


            var i = 0
            while (i < num) {
                val prev: Coordinate?
                val state: Coordinate

                if (i == 0) {
                    state = start
                    prev = startPrev
                } else if (i == 1) {
                    state = steps!![i - 1]
                    prev = startPrev
                } else {
                    state = steps!![i - 1]
                    prev = steps[i - 2]
                }

                val possibleSteps = getPossibleSteps(state, prev)

                if (possibleSteps.size == 0) {
                    if (steps!!.size == 0) {
                        return null
                    }

                    steps = alter(start, startPrev, steps)
                    if (steps == null) {
                        return null
                    }

                    i--
                    i++
                    continue
                }

                val newStep = possibleSteps[0]
                steps!!.add(newStep)
                i++
            }

            return steps
        }

        private fun alter(start: Coordinate, startPrev: Coordinate?, steps: MutableList<Coordinate>): MutableList<Coordinate>? {
            val size = steps.size

            var i = size - 1
            while (i >= 0) {
                val current = steps[i]
                val prev = if (i == 0) start else steps[i - 1]
                val prevprev: Coordinate?
                if (i > 1) {
                    prevprev = steps[i - 2]
                } else if (i == 1) {
                    prevprev = start
                } else {
                    prevprev = startPrev
                }

                val alternatives = getPossibleSteps(prev, prevprev)
                val index = alternatives.indexOf(current)

                if (index != alternatives.size - 1) {
                    val newItem = alternatives[index + 1]
                    steps[i] = newItem

                    val remainder = findFirstLegitSteps(prev, newItem, size - i - 1)
                    if (remainder == null) {
                        i++
                        i--
                        continue
                    }

                    removeAfterIndexExclusive(steps, i)
                    steps.addAll(remainder)

                    return steps
                } else {
                    if (i == 0) {
                        return null
                    }
                }
                i--
            }

            return steps
        }

        companion object {
            private val MAZE_START = Coordinate(0, 0)
            private fun removeAfterIndexExclusive(list: MutableList<*>, index: Int) {
                val rnum = list.size - 1 - index

                for (i in 0 until rnum) {
                    list.removeAt(list.size - 1)
                }
            }
        }
    }

    private fun readTillParsed(inputValue: String): Input? {

        val parser = InputParser()
        var input: Input? = null
        inputValue.lines().forEach { line ->
            input = parser.feedLine(line)
        }

        return input
    }

    fun solve() {
        val output = solver.solve()

        for (c in output.steps) {
            val value = if (c == null) {
                "felvesz"
            } else {
                "${c.x} ${c.y}"
            }
        }
    }
}