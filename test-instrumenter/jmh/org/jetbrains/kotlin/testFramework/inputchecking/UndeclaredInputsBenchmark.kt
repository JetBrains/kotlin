/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.pathString

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class UndeclaredInputsBenchmark {

    private val fileCount = 100_000
    private val tempDir = Files.createTempDirectory("benchmark").toFile().canonicalFile.toPath()
    private val outsideRootDir = tempDir.resolve("outside").createDirectories()
    private val rootDir = tempDir.resolve("root").createDirectories()
    private val buildDir = rootDir.resolve("build").createDirectories()
    private val srcKotlin = rootDir.resolve("src/main/kotlin").createDirectories()

    private lateinit var pathsToCheck: List<String?>
    private lateinit var expectedUndeclaredInputs: Set<String>

    @Setup(Level.Trial)
    fun setUp() {
        val declared = generateDeclaredInputs(8)
        val declaredNonCanonical = generateNonCanonicalDeclared(15)
        val undeclared = generateUndeclaredInputs(11)
        val undeclaredAlreadyDetected = generateAlreadyDetectedUndeclaredInputs(5)
        val undeclaredNonCanonical = generateNonCanonicalUndeclared(15)
        val nulls = generateNulls(1)
        val filesOutsideRootDir = generateFilesOutsideRootDir(20)
        val filesInsideBuildDir = generateFilesInsideBuildDir(20)
        val directories = generateDirectories(5)

        pathsToCheck = buildList {
            addAll(declared)
            addAll(declaredNonCanonical)
            addAll(undeclared)
            addAll(undeclaredAlreadyDetected)
            addAll(undeclaredNonCanonical)
            addAll(nulls)
            addAll(filesOutsideRootDir)
            addAll(filesInsideBuildDir)
            addAll(directories)
        }.shuffled()

        expectedUndeclaredInputs = buildSet {
            addAll(undeclared)
            addAll(undeclaredAlreadyDetected)
            addAll(undeclaredNonCanonical)
        }

        require(pathsToCheck.size == fileCount)

        val declaredInputs = declared + declaredNonCanonical.map { File(it).canonicalPath }
        UndeclaredInputsGuard.install(rootDir.pathString, buildDir.pathString, declaredInputs)

        // preload internal list
        for (it in undeclaredAlreadyDetected) {
            UndeclaredInputsGuard.getInstance().checkPath(it)
        }
    }

    private fun generateDeclaredInputs(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createFile("Declared$it.kt"))
        }
    }

    private fun generateNonCanonicalDeclared(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createFile("NonCanonicalDeclared$it.kt").replace("src/main", "src/../src/main"))
        }
    }

    private fun generateUndeclaredInputs(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createFile("Undeclared$it.kt"))
        }
    }

    private fun generateAlreadyDetectedUndeclaredInputs(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createFile("AlreadyDetectedUndeclared$it.kt"))
        }
    }

    private fun generateNonCanonicalUndeclared(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createFile("NonCanonicalUndeclared$it.kt").replace("src/main", "src/../src/main"))
        }
    }

    private fun generateNulls(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(null)
        }
    }

    private fun generateFilesOutsideRootDir(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(outsideRootDir.createFile("outside$it.txt"))
        }
    }

    private fun generateFilesInsideBuildDir(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(buildDir.createFile("Class$it.class"))
        }
    }

    private fun generateDirectories(n: Int) = buildList {
        repeat(n percentOf fileCount) {
            add(srcKotlin.createDirectory("dir$it"))
        }
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        for (path in pathsToCheck) {
            UndeclaredInputsGuard.getInstance().checkPath(path)
        }
        blackhole.consume(UndeclaredInputsGuard.getInstance().undeclaredInputs)
    }

    @Suppress("unused")
    @TearDown(Level.Invocation)
    fun assertCorrectUndeclaredInputsDetected() {
        val undeclaredInputs = UndeclaredInputsGuard.getInstance().undeclaredInputs

        check(undeclaredInputs == expectedUndeclaredInputs) {
            buildString {
                appendLine("undeclaredInputs != expectedUndeclaredInputs")
                undeclaredInputs.filterNot { it in expectedUndeclaredInputs }.forEach(::appendLine)
            }
        }
    }
}

private fun Path.createFile(file: String) =
    resolve(file).createFile().absolutePathString()

private fun Path.createDirectory(file: String) =
    resolve(file).createDirectories().absolutePathString()

private infix fun Int.percentOf(base: Int): Int =
    base * this / 100
