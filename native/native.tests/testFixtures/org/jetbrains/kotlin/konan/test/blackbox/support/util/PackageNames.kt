/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.renderer.KeywordStringsGenerated.KEYWORDS
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import kotlin.io.path.name
import kotlin.math.min

internal fun computePackageName(testDataBaseDir: File, testDataFile: File): PackageName {
    assertTrue(testDataFile.startsWith(testDataBaseDir)) {
        """
            The file is outside of the directory.
            File: $testDataFile
            Directory: $testDataBaseDir
        """.trimIndent()
    }

    return testDataFile.parentFile
        .relativeTo(testDataBaseDir)
        .resolve(testDataFile.nameWithoutExtension)
        .toPath()
        .map { pathSegment ->
            val packageSegment = pathSegment.name
            if (packageSegment in KEYWORDS)
                "_${packageSegment}_"
            else
                buildString {
                    packageSegment.forEachIndexed { index, ch ->
                        if (ch.isJavaIdentifierPart()) {
                            if (index == 0 && !ch.isJavaIdentifierStart()) {
                                // If the first character is not suitable for start, escape it with '_'.
                                append('_')
                            }
                            append(ch)
                        } else {
                            // Replace incorrect character.
                            append('_')
                        }
                    }
                }
        }
        .let(::PackageName)
}

internal fun Set<PackageName>.findCommonPackageName(): PackageName = when (size) {
    0 -> PackageName.EMPTY
    1 -> first()
    else -> map { packageName -> packageName.segments }
        .reduce { commonSegments: List<String>, segments: List<String> ->
            buildList(min(commonSegments.size, segments.size)) {
                val i = commonSegments.iterator()
                val j = segments.iterator()

                while (i.hasNext() && j.hasNext()) {
                    val segment = i.next()
                    if (segment == j.next()) add(segment) else break
                }
            }
        }
        .let(::PackageName)
}

internal fun joinPackageNames(a: PackageName, b: PackageName): PackageName = when {
    a.isEmpty() -> b
    b.isEmpty() -> a
    else -> PackageName(a.segments + b.segments)
}

internal fun String.prependPackageName(packageName: PackageName): String =
    if (packageName.isEmpty()) this else "$packageName.$this"

internal fun PackageName.startsWith(other: PackageName): Boolean {
    if (segments.size < other.segments.size) return false

    for (i in other.segments.indices) {
        if (segments[i] != other.segments[i]) return false
    }

    return true
}
