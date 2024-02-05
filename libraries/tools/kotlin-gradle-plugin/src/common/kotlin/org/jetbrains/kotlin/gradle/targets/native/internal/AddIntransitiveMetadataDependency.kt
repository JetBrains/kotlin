/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

/**
 * Dependencies here are using a special configuration called 'intransitiveMetadataConfiguration'.
 * This special configuration can tell the IDE that these dependencies shall *not* be transitively visible
 * to dependsOn edges.
 * This is necessary for the way the commonizer handles its "expect refinement" approach.
 * In this mode, every source set will receive exactly one commonized library to analyze its source code with.
 */
internal fun Project.addIntransitiveMetadataDependencyIfPossible(sourceSet: DefaultKotlinSourceSet, dependency: FileCollection) {
    val dependencyConfigurationName = if (project.isIntransitiveMetadataConfigurationEnabled) {
        sourceSet.intransitiveMetadataConfigurationName
    } else {
        @Suppress("DEPRECATION")
        sourceSet.implementationMetadataConfigurationName
    }
    project.dependencies.add(dependencyConfigurationName, dependency)
}
