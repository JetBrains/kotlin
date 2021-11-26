/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

/**
 * Naive sub-optimal implementation of glob expansion.
 */
internal fun expandGlobTo(unexpendedPath: File, output: MutableCollection<File>) {
    val normalizedUnexpandedPath = Paths.get(unexpendedPath.path).toAbsolutePath().normalize()

    val paths = generateSequence(normalizedUnexpandedPath) { it.parent }.toMutableList().apply { reverse() }
    for (index in 1 until paths.size) {
        val path = paths[index]

        val isGlob = '*' in path.name
        if (isGlob) {
            val basePath = paths[index - 1]

            val pattern = basePath.relativize(normalizedUnexpandedPath).toString()
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")

            Files.walkFileTree(basePath, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (matcher.matches(basePath.relativize(file))) output += file.toFile()
                    return FileVisitResult.CONTINUE
                }
            })

            return
        }
    }

    // No globs to expand.
    output += normalizedUnexpandedPath.toFile()
}

internal fun expandGlob(unexpendedPath: File): Collection<File> = buildList { expandGlobTo(unexpendedPath, this) }
