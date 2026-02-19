/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Naive suboptimal implementation of glob expansion.
 */
internal fun expandGlobTo(unexpandedPath: File, output: MutableCollection<File>) {
    assertTrue(unexpandedPath.isAbsolute) { "Path must be absolute: $unexpandedPath" }

    val paths: List<File> = generateSequence(unexpandedPath) { it.parentFile }.toMutableList().apply { reverse() }
    for (index in 1 until paths.size) {
        val path: File = paths[index]

        val isGlob = '*' in path.name
        if (isGlob) {
            val basePath: File = paths[index - 1]
            val basePathAsPath: Path = basePath.toPath()

            val pattern: String = unexpandedPath.relativeTo(basePath).path.let { pattern ->
                if (File.separatorChar == '\\') pattern.replace("\\", "\\\\") else pattern
            }
            val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")

            Files.walkFileTree(basePathAsPath, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (matcher.matches(basePathAsPath.relativize(file))) output += file.toFile()
                    return FileVisitResult.CONTINUE
                }
            })

            return
        }
    }

    // No globs to expand.
    output += unexpandedPath
}

internal fun expandGlob(unexpendedPath: File): Collection<File> = buildList { expandGlobTo(unexpendedPath, this) }
