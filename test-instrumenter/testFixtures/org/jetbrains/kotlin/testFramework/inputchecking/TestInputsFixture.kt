/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking

import jdk.jfr.consumer.RecordingStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

/**
 * Shared setup for [TestInputsChecker] tests and benchmarks.
 *
 * It reproduces the directory layout that the checker cares about:
 *
 * ```
 * .
 * ├── outside
 * └── root
 *     ├── some-project
 *     │   ├── build
 *     │   └── src
 *     │       └── main
 *     │           └── kotlin
 *     └── kotlin-native
 *         └── dist
 *             └── klib
 *                 └── cache
 *                     └── target-gSTATIC-system
 *                         └── stdlib-per-file-cache
 * ```
 */
class TestInputsFixture(
    baseDir: Path,
    val failFast: Boolean = false,
    configure: context(ConfigurationScope) TestInputsFixture.() -> Unit,
) {
    object ConfigurationScope

    private val outsideRootDir: Path = baseDir.resolve("outside")
    private val rootDir: Path = baseDir.resolve("root")
    private val buildDir: Path = rootDir.resolve("some-project/build")
    private val srcKotlin: Path = rootDir.resolve("some-project/src/main/kotlin")
    private val klibCacheDir: Path = rootDir.resolve("kotlin-native/dist/klib/cache")
    private val klibStdlibCacheDir: Path = klibCacheDir.resolve("target-gSTATIC-system/stdlib-per-file-cache")

    val declared: Set<String> field = mutableSetOf()
    val declaredNonCanonical: Set<String> field = mutableSetOf()
    val undeclared: Set<String> field = mutableSetOf()
    val undeclaredAlreadyDetected: Set<String> field = mutableSetOf()
    val undeclaredNonCanonical: Set<String> field = mutableSetOf()
    val nulls: List<String?> field = mutableListOf()
    val filesOutsideRootDir: Set<String> field = mutableSetOf()
    val filesInsideBuildDir: Set<String> field = mutableSetOf()
    val klibCacheFiles: Set<String> field = mutableSetOf()
    val klibStdlibCacheFiles: Set<String> field = mutableSetOf()
    val directories: Set<String> field = mutableSetOf()

    init {
        outsideRootDir.createDirectories()
        rootDir.createDirectories()
        buildDir.createDirectories()
        srcKotlin.createDirectories()
        klibCacheDir.createDirectories()
        klibStdlibCacheDir.createDirectories()
        context(ConfigurationScope) { configure() }
        initialize()
    }

    fun checkAllPaths(): Set<String> =
        captureJfrEvents {
            for (path in allPaths) {
                TestInputsChecker.getInstance().checkPath(path)
            }
        }

    fun reset() {
        initialize()
    }

    private fun initialize() {
        TestInputsChecker.initialize(
            rootDir.toString(),
            buildDir.toString(),
            klibCacheDir.toString(),
            klibStdlibCacheDir.toString(),
            declaredInputs,
            failFast,
        )
    }

    context(_: ConfigurationScope)
    fun createDeclaredFile(suffix: Int? = null) {
        val path = srcKotlin.createFile("Declared${suffix.orEmptyString()}.kt")
        declared.add(path)
    }

    context(_: ConfigurationScope)
    fun createNonCanonicalDeclaredFile(suffix: Int? = null) {
        val path = srcKotlin.createFile("NonCanonicalDeclared${suffix.orEmptyString()}.kt").toNonCanonical()
        declaredNonCanonical.add(path)
    }

    context(_: ConfigurationScope)
    fun createUndeclaredFile(suffix: Int? = null) {
        val path = srcKotlin.createFile("Undeclared${suffix.orEmptyString()}.kt")
        undeclared.add(path)
    }

    context(_: ConfigurationScope)
    fun createAlreadyDetectedUndeclaredFile(suffix: Int? = null) {
        val path = srcKotlin.createFile("AlreadyDetectedUndeclared${suffix.orEmptyString()}.kt")
        undeclaredAlreadyDetected.add(path)
    }

    context(_: ConfigurationScope)
    fun createNonCanonicalUndeclaredFile(suffix: Int? = null) {
        val path = srcKotlin.createFile("NonCanonicalUndeclared${suffix.orEmptyString()}.kt").toNonCanonical()
        undeclaredNonCanonical.add(path)
    }

    context(_: ConfigurationScope)
    fun addNull() {
        nulls.add(null)
    }

    context(_: ConfigurationScope)
    fun createFileOutsideRootDir(suffix: Int? = null) {
        val path = outsideRootDir.createFile("outside${suffix.orEmptyString()}.txt")
        filesOutsideRootDir.add(path)
    }

    context(_: ConfigurationScope)
    fun createFileInsideBuildDir(suffix: Int? = null) {
        val path = buildDir.createFile("Class${suffix.orEmptyString()}.class")
        filesInsideBuildDir.add(path)
    }

    context(_: ConfigurationScope)
    fun createKlibCacheFile(suffix: Int? = null) {
        val path = klibCacheDir.createFile("klib_cache${suffix.orEmptyString()}")
        klibCacheFiles.add(path)
    }

    context(_: ConfigurationScope)
    fun createKlibStdlibCacheFile(suffix: Int? = null) {
        val path = klibStdlibCacheDir.createFile("klib_stdlib_cache${suffix.orEmptyString()}")
        klibStdlibCacheFiles.add(path)
    }

    context(_: ConfigurationScope)
    fun createDirectory(suffix: Int? = null) {
        val path = srcKotlin.createDirectory("dir${suffix.orEmptyString()}")
        directories.add(path)
    }

    val allPaths: List<String?>
        get() = buildList {
            addAll(declared)
            addAll(declaredNonCanonical)
            addAll(undeclared)
            addAll(undeclaredAlreadyDetected)
            addAll(undeclaredNonCanonical)
            addAll(nulls)
            addAll(filesOutsideRootDir)
            addAll(filesInsideBuildDir)
            addAll(klibCacheFiles)
            addAll(klibStdlibCacheFiles)
            addAll(directories)
        }.shuffled()

    val declaredInputs: Set<String>
        get() = buildSet {
            addAll(declared)
            addAll(declaredNonCanonical.map { File(it).canonicalPath })
        }

    val expectedUndeclaredInputs: Set<String>
        get() = buildSet {
            addAll(undeclared)
            addAll(undeclaredNonCanonical)
            addAll(klibStdlibCacheFiles)
        }
}

private fun captureJfrEvents(observedCodeBlock: () -> Unit): Set<String> =
    buildSet {
        RecordingStream().use { stream ->
            stream.enable("jetbrains.UndeclaredInput")
            stream.onEvent("jetbrains.UndeclaredInput") { add(it.getString("path")) }
            stream.startAsync()
            observedCodeBlock()
            stream.stop()
        }
    }

private fun Path.createFile(file: String): String =
    resolve(file).createFile().absolutePathString()

private fun Path.createDirectory(file: String): String =
    resolve(file).createDirectories().absolutePathString()

private fun String.toNonCanonical(): String =
    replace("src/main", "src/../src/main")

private fun Int?.orEmptyString(): String =
    this?.toString().orEmpty()
