/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragment

fun parseIdeaKpmBinaryCoordinates(coordinates: String): IdeaKpmBinaryCoordinates {
    val parts = coordinates.split(":")
    if (parts.size == 3) {
        return IdeaKpmBinaryCoordinatesImpl(parts[0], parts[1], parts[2])
    }

    if (parts.size == 5) {
        return IdeaKpmBinaryCoordinatesImpl(parts[0], parts[1], parts[2], parts[3], parts[4])
    }

    throw IllegalArgumentException("Cannot parse $coordinates into ${IdeaKpmBinaryCoordinates::class.java.simpleName}")
}

fun Iterable<IdeaKpmBinaryCoordinates>.parsableString() =
    joinToString("," + System.lineSeparator(), "", "") { "\"$it\"" }

@Suppress("unused") // Debugging API
fun IdeaKpmFragment.parsableDependencyCoordinatesString(): String {
    return dependencies.filterIsInstance<IdeaKpmBinaryDependency>()
        .mapNotNull { it.coordinates }.toSet()
        .parsableString()
}
