/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.kotlin

import org.jetbrains.kotlin.testFramework.inputchecking.TestInputsChecker
import org.jetbrains.kotlin.testFramework.inputchecking.TestInputsFixture
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class TestInputsCheckerBenchmark {

    private val fileCount = 100_000
    private lateinit var fixture: TestInputsFixture

    private lateinit var expectedUndeclaredInputs: Set<String>
    private lateinit var detectedUndeclaredInputs: Set<String>

    @Suppress("unused")
    @Setup(Level.Trial)
    fun buildFixture() {
        val benchmarkDir = Paths.get("build/benchmark").toFile().canonicalFile.toPath().apply { deleteRecursively() }

        fixture = TestInputsFixture(benchmarkDir) {
            repeat(8 percentOf fileCount) { createDeclaredFile(it) }
            repeat(15 percentOf fileCount) { createNonCanonicalDeclaredFile(it) }
            repeat(11 percentOf fileCount) { createUndeclaredFile(it) }
            repeat(5 percentOf fileCount) { createAlreadyDetectedUndeclaredFile(it) }
            repeat(15 percentOf fileCount) { createNonCanonicalUndeclaredFile(it) }
            repeat(1 percentOf fileCount) { addNull() }
            repeat(20 percentOf fileCount) { createFileOutsideRootDir(it) }
            repeat(10 percentOf fileCount) { createFileInsideBuildDir(it) }
            repeat(5 percentOf fileCount) { createKlibCacheFile(it) }
            repeat(5 percentOf fileCount) { createKlibStdlibCacheFile(it) }
            repeat(5 percentOf fileCount) { createDirectory(it) }
            require(this.allPaths.size == fileCount)
        }

        expectedUndeclaredInputs = fixture.expectedUndeclaredInputs
    }

    @Suppress("unused")
    @Setup(Level.Invocation)
    fun resetState() {
        fixture.reset()
        // preload internal list
        for (it in fixture.undeclaredAlreadyDetected) {
            TestInputsChecker.getInstance().checkPath(it)
        }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        detectedUndeclaredInputs = fixture.checkAllPaths()
        blackhole.consume(detectedUndeclaredInputs)
    }

    @Suppress("unused")
    @TearDown(Level.Invocation)
    fun assertCorrectUndeclaredInputsWereDetected() {
        check(detectedUndeclaredInputs == expectedUndeclaredInputs) {
            val extraUndeclaredInputs = detectedUndeclaredInputs.filterNot { it in expectedUndeclaredInputs }
            val missingUndeclaredInputs = expectedUndeclaredInputs.filterNot { it in detectedUndeclaredInputs }

            buildString {
                appendLine("undeclaredInputs (${detectedUndeclaredInputs.size}) != expectedUndeclaredInputs (${expectedUndeclaredInputs.size})")
                if (extraUndeclaredInputs.isNotEmpty()) {
                    appendLine("Extra undeclared inputs (10/${extraUndeclaredInputs.size}):")
                    extraUndeclaredInputs.take(10).forEach(::appendLine)
                }
                if (missingUndeclaredInputs.isNotEmpty()) {
                    appendLine("Missing undeclared inputs (10/${missingUndeclaredInputs.size}):")
                    missingUndeclaredInputs.take(10).forEach(::appendLine)
                }
            }
        }
    }
}

private infix fun Int.percentOf(base: Int): Int =
    base * this / 100
