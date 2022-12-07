/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyTransformers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency.Companion.SOURCES_BINARY_TYPE
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyTransformer
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeArtifactResolutionQuerySourcesAndDocumentationResolver

/**
 * Filters out -sources.jar and -javadoc.jar dependencies that are actually not necessary for the KotlinSourceSet:
 * This counter-acts the fact that the [IdeArtifactResolutionQuerySourcesAndDocumentationResolver] is potentially "over-resolving" sources
 * for multiplatform libraries.
 */
internal object UnusedSourcesAndDocumentationFilter : IdeDependencyTransformer {

    override fun transform(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>): Set<IdeaKotlinDependency> {
        val binaryDependencies = dependencies.filterIsInstance<IdeaKotlinResolvedBinaryDependency>()

        val classpathBinaryCoordinates = binaryDependencies.filter { it.binaryType == CLASSPATH_BINARY_TYPE }
            .mapNotNull { it.coordinates?.relevantString() }
            .toSet()

        val unusedDependencies = binaryDependencies.filter { it.binaryType in sourcesAndDocumentationBinaryTypes }
            .filter { it.coordinates?.relevantString() !in classpathBinaryCoordinates }
            .toSet()

        return dependencies - unusedDependencies
    }


    private val sourcesAndDocumentationBinaryTypes = setOf(SOURCES_BINARY_TYPE, DOCUMENTATION_BINARY_TYPE)

    /**
     * This filter only works based upon group, module and version.
     * We can consider the sourceSetName to be irrelevant, since we do not have -sources.jar published on a 'per source set' level.
     */
    private fun IdeaKotlinBinaryCoordinates.relevantString(): String = "$group:$module:$version"
}

