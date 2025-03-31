/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.StateRepresentation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test

/**
 * Test [exclusiveFileLock].
 */
class ExclusiveFileLockLincheckTest {

    class Counter {
        private val lockFile: Path =
            lockFileDir.resolve("counter_${Random.nextUInt()}.txt")

        init {
            if (!lockFile.exists()) {
                lockFile.createFile()
                lockFile.writeText("0")
            }
        }

        private var value: Int = 0

        fun inc(): Int {
            return exclusiveFileLock(lockFile) {
                ++value
            }
        }

        fun get(): Int {
            return exclusiveFileLock(lockFile) {
                value
            }
        }

        private val lockFileRelativePath: String =
            lockFile.invariantSeparatorsPathString

        fun stateRepresentation(): String =
            "${get()} (${lockFileRelativePath})"
    }

    private val c: Counter = Counter()

    @Operation
    fun inc(): Int = c.inc()

    @Operation
    fun get(): Int = c.get()

    @StateRepresentation
    fun stateRepresentation(): String = c.stateRepresentation()

    @Test
    fun stressTest() {
        StressOptions()
            .iterations(100)
            .invocationsPerIteration(100)
            .check(this::class)
    }

    @Test
    fun modelCheck() {
        ModelCheckingOptions()
            .iterations(100)
            .invocationsPerIteration(100)
            .check(this::class)
    }

    companion object {
        private val lockFileDir: Path =
            createTempDirectory("ExclusiveFileLockLincheckTest")
    }
}
