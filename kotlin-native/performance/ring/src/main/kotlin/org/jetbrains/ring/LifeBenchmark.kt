package org.jetbrains.ring

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

data class Pos(val i: Int, val j: Int)

data class Cell(val isAlive: Boolean = false)

class Generation(private val width: Int, private val height: Int) {
    val cells = Array(height) { _ -> Array(width) { _ -> Cell() } }

    fun evolve(): Generation {
        val newGen = Generation(width, height)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val neighborhood = mutableListOf<Pos>()
                for (di in -1..1) {
                    for (dj in -1..1) {
                        if (di != 0 || dj != 0) {
                            neighborhood.add(Pos(i + di, j + dj))
                        }
                    }
                }
                assert(neighborhood.size == 8)
                val aliveNeighbours = neighborhood
                        .map { wrapOverEdge(it) }
                        .map { cells[it.i][it.j] }
                        .count { it.isAlive }

                val newAlive =
                        if (cells[i][j].isAlive) aliveNeighbours in 2..3
                        else aliveNeighbours == 3

                newGen.cells[i][j] = Cell(newAlive)
            }
        }

        return newGen
    }

    // good luck in scalar replacement?
    private fun wrapOverEdge(orig: Pos) =
            if (orig.i in 0 until height && orig.j in 0 until width) orig
            else Pos((orig.i + height) % height, (orig.j + width) % width)

    override fun equals(other: Any?): Boolean = when {
        other is Generation -> {
            other.cells.size == cells.size &&
                    cells.zip(other.cells).all { (a, b) -> a contentEquals b }
        }
        else -> false
    }

    companion object {
        fun random(width: Int, height: Int): Generation {
            val gen = Generation(width, height)

            for (i in 0 until height) {
                for (j in 0 until width) {
                    gen.cells[i][j] = Cell(Random.nextInt() % 2 == 0)
                }
            }

            return gen
        }
    }
}

class Universe(val width: Int, val height: Int) {
    var gen = Generation.random(width, height)

    fun evolve() {
        gen = gen.evolve()
    }
}

fun run(space: Int, time: Int) {
    val width = space
    val height = space
    val universe = Universe(width, height)
    for (i in 0 until time) {
        universe.evolve()
    }
    Blackhole.consume(universe)
}

open class LifeBenchmark {
    val spaceScale = BENCHMARK_SIZE / 40
    val timeScale = 5

    fun bench() {
        run(spaceScale, timeScale)
    }
}

class LifeWithMarkHelpersBenchmark : LifeBenchmark() {
    val numberOfMarkHelpers = 5;

    @Volatile
    var done = false
    val markHelpers = Array(numberOfMarkHelpers, { _ -> Worker.start() })
    val markHelperJobs = markHelpers.map {
        it.execute(TransferMode.SAFE, { this }) {
            // run some thread-local work in a loop without allocations or external calls
            fun fib(n: Int): Int {
                if (n == 0) return 0
                var prev = 0
                var cur = 1
                for (i in 2..n) {
                    val next = cur + prev
                    prev = cur
                    cur = next
                }
                return cur
            }

            var sum = 0
            while (!it.done) {
                sum += fib(100)
            }
            return@execute sum
        }
    }

    fun terminate() {
        done = true
        markHelperJobs.forEach { it.result }
        markHelpers.forEach { it.requestTermination().result }
    }
}

