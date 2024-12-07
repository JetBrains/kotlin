/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty

/**
 * Service interface that allows resolving dependencies for the IDE during Gradle sync
 *
 * #### Entry point
 * [IdeMultiplatformImport]: Will allow registering implementations of this resolver using [IdeMultiplatformImport.registerDependencyResolver]
 *
 * #### Api Contract:
 * A resolver always has to resolve *all* visible source sets for a given [KotlinSourceSet] as Set of [IdeaKotlinDependency]
 * This includes dependencies that are transitively visible through the SourceSets dependsOn closure:
 *
 * e.g. if a resolver is invoked on a SourceSet structure like
 * ```
 * commonMain
 * dependencies(libraryA)
 *     ^
 *     intermediateMain
 *     dependencies(libraryB)
 *          ^
 *          jvmMain
 *          dependencies(libraryC)
 * ```
 *
 * Then calling the resolver on _jvmMain_ shall return setOf(libraryA, libraryB, libraryC)
 *
 * #### Resolvers that rely on task execution
 * It might happen that some implementation of an [IdeDependencyResolver] has to rely on the execution
 * of certain build tasks before it can return the dependencies. In order to tell the import subsystem to execute
 * such tasks before Gradle sync (and therefore the resolver) is executed one can implement the [WithBuildDependencies] interface
 */
@ExternalKotlinTargetApi
fun interface IdeDependencyResolver {
    fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency>

    @ExternalKotlinTargetApi
    interface WithBuildDependencies {
        /**
         * return anything accepted to be passed to [org.gradle.api.Task.dependsOn]
         *
         * #### Example: Resolver relying on a single task to be executed:
         * ```kotlin
         * object MyResolver : IdeDependencyResolver, WithBuildDependencies {
         *     fun resolve(sourceSet: KotlinSourceSet) = // ...
         *     fun dependencies(project: Project) = listOf(project.tasks.named("myTask"))
         * }
         * ```
         */
        fun dependencies(project: Project): Iterable<Any>
    }

    @ExternalKotlinTargetApi
    companion object {

        /**
         * [IdeDependencyResolver] that will just return an empty Set of dependencies (noop)
         */
        val empty = IdeDependencyResolver { emptySet() }

        /**
         * Special binaryType String that indicates that a certain dependency is only a sources.jar and
         * therefore should be attached as extra to the binary dependency.
         * This is only necessary for resolvers that implement sources/documentation resolution as [IdeDependencyResolver]
         * instead of [IdeAdditionalArtifactResolver]
         */
        const val SOURCES_BINARY_TYPE = "SOURCES"

        /**
         * Special binaryType String that indicates that a certain dependency is only a javadoc.jar and
         * therefore should be attached as extra to the binary dependency.
         * This is only necessary for resolvers that implement sources/documentation resolution as [IdeDependencyResolver]
         * instead of [IdeAdditionalArtifactResolver]
         */
        const val DOCUMENTATION_BINARY_TYPE = "DOCUMENTATION"

        /**
         * Extra on [IdeaKotlinDependency] to attach the resolver which created/resolved a certain dependency.
         * This can be used for debugging and testing
         */
        var IdeaKotlinDependency.resolvedBy: IdeDependencyResolver? by extrasReadWriteProperty("resolvedBy")

        /**
         * Extra on [IdeaKotlinDependency] to attach the [ResolvedArtifactResult] that which was used to create a certain dependency.
         * This can be used for debugging and testing
         */
        var IdeaKotlinDependency.gradleArtifact: ResolvedArtifactResult? by extrasReadWriteProperty("gradleArtifact")
    }
}

/**
 * Creates a composite [IdeDependencyResolver] from the specified [resolvers]
 * Resolvers that are `null` will be ignored.
 * The composite will preserve the order and invoke the [resolvers] in the same order as specified.
 * The resulting set of dependencies will be the superset of all results of individual resolvers.
 */
@ExternalKotlinTargetApi
fun IdeDependencyResolver(
    resolvers: Iterable<IdeDependencyResolver?>,
): IdeDependencyResolver {
    val resolversList = resolvers.filterNotNull()
    if (resolversList.isEmpty()) return IdeDependencyResolver.empty
    return IdeCompositeDependencyResolver(resolversList)
}

/**
 * Creates a composite [IdeDependencyResolver] from the specified [resolvers]
 * Resolvers that are `null` will be ignored.
 * The composite will preserve the order and invoke the [resolvers] in the same order as specified.
 * The resulting set of dependencies will be the superset of all results of individual resolvers.
 */
@ExternalKotlinTargetApi
fun IdeDependencyResolver(
    vararg resolvers: IdeDependencyResolver?,
): IdeDependencyResolver = IdeDependencyResolver(resolvers.toList())

private class IdeCompositeDependencyResolver(
    val children: List<IdeDependencyResolver>,
) : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return children.flatMap { child -> child.resolve(sourceSet) }.toSet()
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return children.flatMap { child ->
            if (child is IdeDependencyResolver.WithBuildDependencies) child.dependencies(project) else emptyList()
        }
    }
}
