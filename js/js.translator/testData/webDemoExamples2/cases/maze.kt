/**
 * Let's Walk Through a Maze.
 *
 * Imagine there is a maze whose walls are the big 'O' letters.
 * Now, I stand where a big 'I' stands and some cool prize lies
 * somewhere marked with a '$' sign. Like this:
 *
 *    OOOOOOOOOOOOOOOOO
 *    O               O
 *    O$  O           O
 *    OOOOO           O
 *    O               O
 *    O  OOOOOOOOOOOOOO
 *    O           O I O
 *    O               O
 *    OOOOOOOOOOOOOOOOO
 *
 * I want to get the prize, and this program helps me do so as soon
 * as I possibly can by finding a shortest path through the maze.
 */
import java.util.*

/**
 * This function looks for a path from max.start to maze.end through
 * free space (a path does not go through walls). One can move only
 * straightly up, down, left or right, no diagonal moves allowed.
 */
fun findPath(maze: Maze): List<Pair<Int, Int>>? {
    val previous = HashMap<Pair<Int, Int>, Pair<Int, Int>>

    val queue = LinkedList<Pair<Int, Int>>
    val visited = HashSet<Pair<Int, Int>>

    queue.offer(maze.start)
    visited.add(maze.start)
    while (!queue.isEmpty()) {
        val cell = queue.poll()
        if (cell == maze.end) break

        for (newCell in maze.neighbors(cell._1, cell._2)) {
            if (newCell in visited) continue
            previous[newCell] = cell
            queue.offer(newCell)
            visited.add(cell)
        }
    }

    if (previous[maze.end] == null) return null

    val path = ArrayList<Pair<Int, Int>>()
    var current = previous[maze.end]
    while (current != maze.start) {
        path.add(0, current)
        current = previous[current]
    }
    return path
}

/**
 * Find neighbors of the (i, j) cell that are not walls
 */
fun Maze.neighbors(i: Int, j: Int): List<Pair<Int, Int>> {
    val result = ArrayList<Pair<Int, Int>>
    addIfFree(i - 1, j, result)
    addIfFree(i, j - 1, result)
    addIfFree(i + 1, j, result)
    addIfFree(i, j + 1, result)
    return result
}

fun Maze.addIfFree(i: Int, j: Int, result: List<Pair<Int, Int>>) {
    if (i !in 0..height - 1) return
    if (j !in 0..width - 1) return
    if (walls[i][j]) return

    result.add(Pair(i, j))
}

/**
 * A data class that represents a maze
 */
class Maze(
        // Number or columns
        val width: Int,
        // Number of rows
        val height: Int,
        // true for a wall, false for free space
        val walls: Array<out Array<out Boolean>>,
        // The starting point (must not be a wall)
        val start: Pair<Int, Int>,
        // The target point (must not be a wall)
        val end: Pair<Int, Int>
) {
}

/** A few maze examples here */
fun main(args: Array<String>) {
    printMaze("I  $")
    printMaze("I O $")
    printMaze("""
    O  $
    O
    O
    O
    O           I
  """)
    printMaze("""
    OOOOOOOOOOO
    O $       O
    OOOOOOO OOO
    O         O
    OOOOO OOOOO
    O         O
    O OOOOOOOOO
    O        OO
    OOOOOO   IO
  """)
    printMaze("""
    OOOOOOOOOOOOOOOOO
    O               O
    O$  O           O
    OOOOO           O
    O               O
    O  OOOOOOOOOOOOOO
    O           O I O
    O               O
    OOOOOOOOOOOOOOOOO
  """)
}

// UTILITIES

fun printMaze(str: String) {
    val maze = makeMaze(str)

    println("Maze:")
    val path = findPath(maze)
    for (i in 0..maze.height - 1) {
        for (j in 0..maze.width - 1) {
            val cell = Pair(i, j)
            print(
                    if (maze.walls[i][j]) "O"
                    else if (cell == maze.start) "I"
                    else if (cell == maze.end) "$"
                    else if (path != null && path.contains(cell)) "~"
                    else " "
            )
        }
        println("")
    }
    println("Result: " + if (path == null) "No path" else "Path found")
    println("")
}


/**
 * A maze is encoded in the string s: the big 'O' letters are walls.
 * I stand where a big 'I' stands and the prize is marked with
 * a '$' sign.
 *
 * Example:
 *
 *    OOOOOOOOOOOOOOOOO
 *    O               O
 *    O$  O           O
 *    OOOOO           O
 *    O               O
 *    O  OOOOOOOOOOOOOO
 *    O           O I O
 *    O               O
 *    OOOOOOOOOOOOOOOOO
 */
fun makeMaze(s: String): Maze {
    val lines = s.split("\n")!!
    val w = max<String?>(lines.toList(), comparator<String?> { o1, o2 ->
        val l1: Int = o1?.size ?: 0
        val l2 = o2?.size ?: 0
        l1 - l2
    })!!
    val data = Array<Array<Boolean>>(lines.size) { Array<Boolean>(w.size) { false } }

    var start: Pair<Int, Int>? = null
    var end: Pair<Int, Int>? = null

    for (line in lines.indices) {
        for (x in lines[line].indices) {
            val c = lines[line]!![x]
            data[line][x] = c == 'O'
            when (c) {
                'I' -> start = Pair(line, x)
                '$' -> end = Pair(line, x)
                else -> {
                }
            }
        }
    }

    if (start == null) {
        throw IllegalArgumentException("No starting point in the maze (should be indicated with 'I')")
    }

    if (end == null) {
        throw IllegalArgumentException("No goal point in the maze (should be indicated with a '$' sign)")
    }

    return Maze(w.size, lines.size, data, start!!, end!!)
}


// An excerpt from the Standard Library
val String?.indices: IntRange get() = IntRange(0, this!!.size)

fun <K, V> Map<K, V>.set(k: K, v: V) {
    put(k, v)
}

fun comparator<T> (f: (T, T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(o1: T, o2: T): Int = f(o1, o2)
    override fun equals(p: Any?): Boolean = false
}

fun <T, C : Collection<T>> Array<T>.to(result: C): C {
    for (elem in this)
        result.add(elem)
    return result
}
