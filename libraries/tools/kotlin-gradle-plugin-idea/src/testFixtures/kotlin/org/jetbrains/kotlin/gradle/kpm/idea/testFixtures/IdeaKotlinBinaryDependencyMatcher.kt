/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinResolvedBinaryDependency
import java.io.File

fun buildIdeaKotlinBinaryDependencyMatchers(notation: Any?): List<IdeaKotlinBinaryDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is IdeaKotlinBinaryDependencyMatcher -> listOf(notation)
        is String -> listOf(IdeaKotlinBinaryDependencyMatcher.Coordinates(parseIdeaKotlinBinaryCoordinates(notation)))
        is Regex -> listOf(IdeaKotlinBinaryDependencyMatcher.CoordinatesRegex(notation))
        is File -> listOf(IdeaKotlinBinaryDependencyMatcher.BinaryFile(notation))
        is Iterable<*> -> notation.flatMap { child -> buildIdeaKotlinBinaryDependencyMatchers(child) }
        else -> error("Can't build ${IdeaKotlinBinaryDependencyMatcher::class.simpleName} from $notation")
    }
}

interface IdeaKotlinBinaryDependencyMatcher : IdeaKotlinDependencyMatcher<IdeaKotlinBinaryDependency>{
    class Coordinates(
        private val coordinates: IdeaKotlinBinaryCoordinates
    ) : IdeaKotlinBinaryDependencyMatcher {
        override val description: String = coordinates.toString()

        override fun matches(dependency: IdeaKotlinBinaryDependency): Boolean {
            return coordinates == dependency.coordinates
        }
    }

    class CoordinatesRegex(
        private val regex: Regex
    ) : IdeaKotlinBinaryDependencyMatcher {
        override val description: String = regex.pattern

        override fun matches(dependency: IdeaKotlinBinaryDependency): Boolean {
            return regex.matches(dependency.coordinates.toString())
        }
    }

    class BinaryFile(
        private val binaryFile: File
    ) : IdeaKotlinBinaryDependencyMatcher {
        override val description: String = binaryFile.path

        override fun matches(dependency: IdeaKotlinBinaryDependency): Boolean {
            return dependency is IdeaKotlinResolvedBinaryDependency && dependency.binaryFile == binaryFile
        }
    }

    class InDirectory(
        private val parentFile: File
    ) : IdeaKotlinBinaryDependencyMatcher {
        constructor(parentFilePath: String) : this(File(parentFilePath))

        override val description: String = "$parentFile/**"

        override fun matches(dependency: IdeaKotlinBinaryDependency): Boolean {
            return dependency is IdeaKotlinResolvedBinaryDependency &&
                    dependency.binaryFile.absoluteFile.normalize().canonicalPath.startsWith(
                        parentFile.absoluteFile.normalize().canonicalPath
                    )
        }
    }
}
