/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.documentationClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.SOURCES_BINARY_TYPE

/**
 * Resolver for attaching additional artifacts to already resolved dependencies.
 * #### Example
 * ```
 * val sourcesJarResolver = IdeAdditionalArtifactResolver { sourceSet, dependencies ->
 *     dependencies.forEach { dependency ->
 *         dependency.sourcesClasspath.add(findMySourcesJarFile(dependency))
 *     }
 * }
 * ```
 */
@ExternalKotlinTargetApi
fun interface IdeAdditionalArtifactResolver {
    /**
     * This function is intended to resolve 'additional' artifacts:
     * This means 'artifacts' that can be attached to some existing/already resolved dependency.
     * One good example of such an 'additional artifact' would be a -sources.jar file:
     * It is not a dependency on its own: It shares the coordinates with some [IdeaKotlinBinaryDependency] and can be
     * attached to this dependency to provide extra functionality.
     *
     * Contract:
     * - This function is allowed to attach data to a given [IdeaKotlinDependency]
     * - This function is not allowed to remove data from a given [IdeaKotlinBinaryDependency]
     * - This function is not supposed to modify the [sourceSet]
     *
     * @param sourceSet: The current SourceSet which shall resolve additional dependencies
     * @param dependencies: The already resolved dependencies from prior stages
     */
    fun resolve(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>)

    @ExternalKotlinTargetApi
    companion object {
        /**
         * Empty [IdeAdditionalArtifactResolver] that will not resolve any artifact (noop)
         */
        val empty = IdeAdditionalArtifactResolver { _, _ -> }
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
internal fun IdeAdditionalArtifactResolver(resolver: IdeDependencyResolver) = IdeAdditionalArtifactResolver { sourceSet, dependencies ->
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


internal fun IdeDependencyResolver.asAdditionalArtifactResolver() = IdeAdditionalArtifactResolver(this)