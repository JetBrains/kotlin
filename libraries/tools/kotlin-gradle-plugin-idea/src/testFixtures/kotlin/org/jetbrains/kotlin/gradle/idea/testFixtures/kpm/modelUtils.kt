/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmBinaryDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragment

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
