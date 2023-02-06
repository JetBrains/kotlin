/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.documentationClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.SOURCES_BINARY_TYPE


fun interface IdeAdditionalArtifactResolver {
    fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>)

    object Empty : IdeAdditionalArtifactResolver {
        override fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) = Unit
    }
}

internal fun IdeDependencyResolver.withAdditionalArtifactResolver(resolver: IdeAdditionalArtifactResolver) =
    IdeDependencyResolver { sourceSet ->
        this@withAdditionalArtifactResolver.resolve(sourceSet).also { result ->
            resolver.resolve(sourceSet, result)
        }
    }

internal fun IdeAdditionalArtifactResolver(resolvers: Iterable<IdeAdditionalArtifactResolver?>) =
    IdeAdditionalArtifactResolver { sourceSet, dependencies ->
        resolvers.forEach { resolver -> resolver?.resolve(sourceSet, dependencies) }
    }


/**
 * Creates an [IdeAdditionalArtifactResolver] from a given [IdeDependencyResolver]:
 * Dependencies from the [IdeDependencyResolver] need to resolve sources and javadoc using
 * the [SOURCES_BINARY_TYPE] or [DOCUMENTATION_BINARY_TYPE]
 */
fun IdeAdditionalArtifactResolver(resolver: IdeDependencyResolver) = IdeAdditionalArtifactResolver { sourceSet, dependencies ->
    /*
    Group already resolved dependencies by their coordinates (ignoring sourceSetName, since -sources.jar are not published
    on a "per source set" level.)
     */
    val dependenciesByCoordinates = dependencies.filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
        .filter { it.binaryType == IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE }
        .groupBy { it.coordinates?.copy(sourceSetName = null) }

    /*
    Use the passed resolver to resolve the -sources.jar and -javadoc jar dependencies as idea dependencies.
    For each dependency, we will find the dependencies to add this artifacts by matching the coordinates.
     */
    resolver.resolve(sourceSet).filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
        .filter { it.binaryType == SOURCES_BINARY_TYPE || it.binaryType == DOCUMENTATION_BINARY_TYPE }
        .forEach forEachSourceOrDocumentationDependency@{ sourceOrDocumentationDependency ->
            /* Find dependencies that match by coordinates and add the artifacts */
            dependenciesByCoordinates[sourceOrDocumentationDependency.coordinates ?: return@forEachSourceOrDocumentationDependency]
                .orEmpty().forEach forEachMatchedDependency@{ dependency ->
                    val classpath = when (sourceOrDocumentationDependency.binaryType) {
                        SOURCES_BINARY_TYPE -> dependency.sourcesClasspath
                        DOCUMENTATION_BINARY_TYPE -> dependency.documentationClasspath
                        else -> return@forEachMatchedDependency
                    }
                    classpath.addAll(dependency.classpath)
                }
        }
}


fun IdeDependencyResolver.asAdditionalArtifactResolver() = IdeAdditionalArtifactResolver(this)