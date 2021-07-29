/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency

/**
 * Reorder the compiler plugin dependencies in this [Configuration] so that dependencies on the
 * modules under [prioritizedPluginArtifactCoordinates] appear first.
 *
 * In particular, the serialization plugin needs to appear first on the -Xplugin classpath in order to avoid conflicts with other compiler
 * plugins producing unexpected IR.
 *
 * KT-47921
 */
internal fun Configuration.reorderPluginClasspathDependencies() {
    withDependencies { dependencySet ->
        val orderedDependencies = dependencySet.toList().partition(Dependency::isPrioritized).toList().flatten()
        dependencySet.clear()
        dependencySet.addAll(orderedDependencies)
    }
}

private val Dependency.isPrioritized: Boolean
    get() = this is ModuleDependency && (group to name) in prioritizedPluginArtifactCoordinates

private val prioritizedPluginArtifactCoordinates = setOf(
    "org.jetbrains.kotlin" to "kotlin-serialization",
    "org.jetbrains.kotlin" to "kotlin-serialization-unshaded"
)