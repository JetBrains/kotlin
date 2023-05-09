/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import kotlin.math.*
import org.jetbrains.benchmarksLauncher.Random

private data class Cell(var isAlive: Boolean = false)

private class Generation(private val width: Int, private val height: Int) {
    var cells = Array(height) { _ -> Array(width) { _ -> Cell() } }
    var prevCells = Array(height) { _ -> Array(width) { _ -> Cell() } }

    fun evolve(): Unit {
        val newCells = prevCells
        prevCells = cells
        cells = newCells

        for (i in 0 until height) {
            for (j in 0 until width) {
                var aliveNeighbours = 0
                for (di in -1..1) {
                    for (dj in -1..1) {
                        if (di != 0 || dj != 0) {
                            var wrappedI = i + di
                            var wrappedJ = j + dj
                            if (wrappedI !in 0 until height || wrappedJ !in 0 until width) {
                                wrappedI = (wrappedI + height) % height
                                wrappedJ = (wrappedJ + width) % width
                            }

                            if (prevCells[wrappedI][wrappedJ].isAlive) {
                                aliveNeighbours += 1
                            }
                        }
                    }
                }

                val newAlive =
                        if (prevCells[i][j].isAlive) aliveNeighbours in 2..3
                        else aliveNeighbours == 3

                newCells[i][j] = Cell(newAlive)
            }
        }
    }

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
                    gen.cells[i][j].isAlive = (Random.nextInt(2) == 0)
                }
            }

            return gen
        }
    }
}

class Life(scale: Int) : Workload {
    private val size = sqrt(scale.toDouble()).toInt() * sqrt(1024.toDouble()).toInt()

    private var gen = Generation.random(size, size)

    companion object : WorkloadProvider<Life> {
        override fun name(): String = "Life"
        override fun allocate(scale: Int) = Life(scale)
    }
}
