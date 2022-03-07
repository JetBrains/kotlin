/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinResolvedBinaryDependency
import java.io.File

fun buildIdeaKotlinDependencyMatchers(any: Any?): List<IdeaKotlinDependencyMatcher> {
    return when (any) {
        null -> emptyList()
        is IdeaKotlinDependencyMatcher -> listOf(any)
        is String -> listOf(IdeaKotlinDependencyMatcher.Coordinates(parseIdeaKotlinBinaryCoordinates(any)))
        is Regex -> listOf(IdeaKotlinDependencyMatcher.CoordinatesRegex(any))
        is File -> listOf(IdeaKotlinDependencyMatcher.BinaryFile(any))
        is Iterable<*> -> any.flatMap { child -> buildIdeaKotlinDependencyMatchers(child) }
        else -> error("Can't build ${IdeaKotlinDependencyMatcher::class.simpleName} from $any")
    }
}

interface IdeaKotlinDependencyMatcher {
    val description: String
    fun matches(dependency: IdeaKotlinDependency): Boolean

    class Coordinates(
        private val coordinates: IdeaKotlinBinaryCoordinates
    ) : IdeaKotlinDependencyMatcher {
        override val description: String = coordinates.toString()

        override fun matches(dependency: IdeaKotlinDependency): Boolean {
            return dependency is IdeaKotlinBinaryDependency && coordinates == dependency.coordinates
        }
    }

    class CoordinatesRegex(
        private val regex: Regex
    ) : IdeaKotlinDependencyMatcher {
        override val description: String = regex.pattern

        override fun matches(dependency: IdeaKotlinDependency): Boolean {
            return dependency is IdeaKotlinBinaryDependency && regex.matches(dependency.coordinates.toString())
        }
    }

    class BinaryFile(
        private val binaryFile: File
    ) : IdeaKotlinDependencyMatcher {
        override val description: String = binaryFile.path

        override fun matches(dependency: IdeaKotlinDependency): Boolean {
            return dependency is IdeaKotlinResolvedBinaryDependency && dependency.binaryFile == binaryFile
        }
    }

    class InDirectory(
        private val parentFile: File
    ) : IdeaKotlinDependencyMatcher {
        constructor(parentFilePath: String) : this(File(parentFilePath))

        override val description: String = "$parentFile/**"

        override fun matches(dependency: IdeaKotlinDependency): Boolean {
            return dependency is IdeaKotlinResolvedBinaryDependency &&
                    dependency.binaryFile.absoluteFile.normalize().canonicalPath.startsWith(
                        parentFile.absoluteFile.normalize().canonicalPath
                    )
        }
    }
}

