/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal fun KotlinSourceSet.checkForCircularDependsOnEdges(other: KotlinSourceSet): Nothing? {
    val stack = mutableListOf(this)
    val visited = hashSetOf<KotlinSourceSet>()

    fun checkReachableRecursively(from: KotlinSourceSet) {
        if (!visited.add(from)) return
        stack += from
        if (this == from) throw InvalidUserCodeException(
            "Circular dependsOn hierarchy found in the Kotlin source sets: ${(stack.toList()).joinToString(" -> ") { it.name }}"
        )
        from.dependsOn.forEach { next -> checkReachableRecursively(next) }
        stack -= from
    }

    checkReachableRecursively(other)
    return null
}

