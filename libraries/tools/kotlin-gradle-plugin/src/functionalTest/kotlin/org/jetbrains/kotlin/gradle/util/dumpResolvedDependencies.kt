/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.UnresolvedDependencyResult

fun Configuration.dumpResolvedDependencies(): String {
    val resolutionResult = incoming.resolutionResult
    val rootComponent = resolutionResult.root
    val dependencies = resolutionResult.allComponents
        .minus(rootComponent)
        .map { it.id.displayName }
        .sorted()
    val unresolved = resolutionResult
        .allDependencies
        .filterIsInstance<UnresolvedDependencyResult>().map {
            it.failure.allCauses.joinToString { it.message ?: "null" }
        }
    return dependencies.plus(unresolved).joinToString("\n")
}