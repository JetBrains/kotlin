@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)
package org.jetbrains.ring

import kotlin.concurrent.*
import kotlin.native.concurrent.*
import kotlin.random.Random
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

// Use the same seed for reproducibility
val rnd = Random(6581)

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
                @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
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
                    gen.cells[i][j] = Cell(rnd.nextInt(100) % 2 == 0)
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

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LifeHideName : SkipWhenBaseOnly() {
    val spaceScale = BENCHMARK_SIZE / 40
    val timeScale = 5
    private val universe = Universe(spaceScale, spaceScale)

    @Benchmark
    fun Life(bh: Blackhole) {
        skipWhenBaseOnly()
        repeat(timeScale) {
            universe.evolve()
        }
        bh.consume(universe)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LifeWithMarkHelpersHideName : SkipWhenBaseOnly() {
    val spaceScale = BENCHMARK_SIZE / 40
    val timeScale = 5
    val numberOfMarkHelpers = 5;
    private val universe = Universe(spaceScale, spaceScale)

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

    @Benchmark
    fun LifeWithMarkHelpers(bh: Blackhole) {
        skipWhenBaseOnly()
        repeat(timeScale) {
            universe.evolve()
        }
        bh.consume(universe)
    }

    @TearDown
    fun terminate() {
        done = true
        markHelperJobs.forEach { it.result }
        markHelpers.forEach { it.requestTermination().result }
    }
}

