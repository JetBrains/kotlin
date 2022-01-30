/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*
import kotlin.streams.asSequence
import kotlin.streams.toList

/**
 * Find the file with given [name] in current [Path].
 *
 * @return `null` if file is absent in current [Path]
 */
internal fun Path.findInPath(name: String): Path? = Files
    .walk(this).use { stream -> stream.asSequence().find { it.fileName.toString() == name } }

/**
 * Find all Kotlin source files (ends with '.kt') in the given [Path].
 */
internal val Path.allKotlinSources: List<Path>
    get() = Files.walk(this)
        .use { files ->
            files.asSequence()
                .filter { it.isRegularFile() && it.extension == "kt" }
                .toList()
        }

/**
 * Find all Java source files (ends with '.java') in the given [Path].
 */
internal val Path.allJavaSources: List<Path>
    get() = Files.walk(this)
        .use { files ->
            files.asSequence()
                .filter { it.isRegularFile() && it.extension == "java" }
                .toList()
        }

/**
 * Create a temporary directory that will be cleaned up on normal JVM termination, but will be left on non-zero exit status.
 *
 * Prefer using JUnit5 `@TempDir` over this method when possible.
 */
internal fun createTempDirDeleteOnExit(prefix: String): Path = Files
    .createTempDirectory(prefix)
    .apply { toFile().deleteOnExit() }

/**
 * Returns list of all files whose name ends with [ext] extension. The comparison is case-insensitive.
 */
internal fun Path.allFilesWithExtension(ext: String): List<Path> =
    Files.walk(this).use { stream -> stream.filter { it.extension.equals(ext, ignoreCase = true) }.toList() }

internal val Path.allKotlinFiles
    get() = allFilesWithExtension("kt")

internal fun Iterable<Path>.relativizeTo(basePath: Path): Iterable<Path> = map {
    it.relativeTo(basePath)
}

internal fun String.normalizePath() = replace("\\", "/")

internal fun Path.copyRecursively(dest: Path) {
    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            dest.resolve(relativize(dir)).createDirectories()
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            file.copyTo(dest.resolve(relativize(file)))
            return FileVisitResult.CONTINUE
        }
    })
}

internal fun Path.deleteRecursively() {
    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun visitFile(
            file: Path,
            fileAttributes: BasicFileAttributes
        ): FileVisitResult {
            file.deleteIfExists()
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(
            dir: Path,
            exception: IOException?
        ): FileVisitResult {
            dir.deleteIfExists()
            return FileVisitResult.CONTINUE
        }
    })
}

internal fun Iterable<String>.toPaths(): List<Path> = map { Paths.get(it) }
