/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinFragment

fun parseIdeaKotlinBinaryCoordinates(coordinates: String): IdeaKotlinBinaryCoordinates {
    val parts = coordinates.split(":")
    if (parts.size == 3) {
        return IdeaKotlinBinaryCoordinatesImpl(parts[0], parts[1], parts[2])
    }

    if (parts.size == 5) {
        return IdeaKotlinBinaryCoordinatesImpl(parts[0], parts[1], parts[2], parts[3], parts[4])
    }

    throw IllegalArgumentException("Cannot parse $coordinates into ${IdeaKotlinBinaryCoordinates::class.java.simpleName}")
}

fun Iterable<IdeaKotlinBinaryCoordinates>.parsableString() =
    joinToString("," + System.lineSeparator(), "", "") { "\"$it\"" }

@Suppress("unused") // Debugging API
fun IdeaKotlinFragment.parsableDependencyCoordinatesString(): String {
    return dependencies.filterIsInstance<IdeaKotlinBinaryDependency>()
        .mapNotNull { it.coordinates }.toSet()
        .parsableString()
}
