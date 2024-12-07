/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

internal fun KotlinSourceSet.checkForCircularDependsOnEdges(other: KotlinSourceSet): Nothing? {
    val stack = mutableListOf(this)
    val visited = hashSetOf<KotlinSourceSet>()

    fun checkReachableRecursively(from: KotlinSourceSet) {
        if (!visited.add(from)) return
        stack += from
        if (this == from) {
            // CircularDependsOnEdges has severity FATAL, so this call will throw an exception
            project.reportDiagnostic(KotlinToolingDiagnostics.CircularDependsOnEdges(stack.map { it.name }))
        }
        from.dependsOn.forEach { next -> checkReachableRecursively(next) }
        stack -= from
    }

    checkReachableRecursively(other)
    return null
}

