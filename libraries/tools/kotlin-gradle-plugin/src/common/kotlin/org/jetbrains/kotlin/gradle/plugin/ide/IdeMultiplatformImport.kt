/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializationExtensionBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.DependencyResolutionLevel.Default
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.tooling.core.Extras

interface IdeMultiplatformImport {

    fun resolveDependencies(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency>

    fun <T : Any> serialize(key: Extras.Key<T>, value: T): ByteArray?

    @ExternalKotlinTargetApi
    fun registerDependencyResolver(
        resolver: IdeDependencyResolver,
        constraint: SourceSetConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel = Default,
    )

    @ExternalKotlinTargetApi
    fun registerDependencyTransformer(
        transformer: IdeDependencyTransformer,
        constraint: SourceSetConstraint,
        phase: DependencyTransformationPhase
    )

    @ExternalKotlinTargetApi
    fun registerDependencyEffect(
        effect: IdeDependencyEffect,
        constraint: SourceSetConstraint
    )

    @ExternalKotlinTargetApi
    fun registerExtrasSerializationExtension(
        extension: IdeaExtrasSerializationExtension
    )

    /**
     * Any [IdeDependencyResolver] has to be registered for a given dependency resolution phase in which it participates
     * The resolution phases will be executed in the order of their enum's ordinal.
     */
    enum class DependencyResolutionPhase {
        /**
         * Generic phase intended to run before all other resolvers
         */
        PreDependencyResolution,

        /**
         * Resolution phase intended to resolve project to project (source) dependencies
         */
        SourceDependencyResolution,

        /**
         * Resolution phase intended to resolve binary dependencies (downloading and transforming from repositories)
         */
        BinaryDependencyResolution,

        /**
         * Generic phase intended to run after all other resolvers.
         */
        PostDependencyResolution
    }

    /**
     * Any [IdeDependencyResolver] has to be registered specifying a certain resolution level.
     * Generally, all resolvers registered in a given resolution level will work collaboratively, meaning the dependency resolution
     * result is the aggregation of all resolvers running.
     *
     * However, only the resolvers in the highest resolution result will run e.g.
     * If resolvers with level [Overwrite] are found, then only those will contribute to the dependency resolution.
     * Otherwise, all [Default] resolvers will run.
     */
    enum class DependencyResolutionLevel {
        Default, Overwrite
    }

    /**
     * Any [IdeDependencyResolver] has to be registered for a given transformation phase.
     * The phases will be executed in the order of this enums ordinal.
     */
    enum class DependencyTransformationPhase {
        /**
         * Generic dependency transformation phase, intended to run a transformation before all other transformers.
         */
        PreDependencyTransformationPhase,

        /**
         * Dependency transformation phase that is free entirely free in its transformation type.
         * Note: Adding dependencies to the resolution result might most likely better be modelled as [IdeDependencyResolver]
         */
        FreeDependencyTransformationPhase,

        /**
         * Special dependency transformation phase intended for filtering dependencies.
         * This phase is guaranteed to run after the [FreeDependencyTransformationPhase] alongside other dependency filtering
         * transformations. Adding filters here is the safest.
         */
        DependencyFilteringPhase,

        /**
         * Generic dependency transformation phase, intended to run a transformation after all other transformers
         */
        PostDependencyTransformationPhase
    }

    /**
     * Used for scoping [IdeDependencyResolver], [IdeDependencyResolver] and [IdeDependencyEffect]
     */
    fun interface SourceSetConstraint {
        operator fun invoke(sourceSet: KotlinSourceSet): Boolean

        companion object {
            val unconstrained = SourceSetConstraint { true }
            val isNative = SourceSetConstraint { sourceSet -> isNativeSourceSet(sourceSet) }
        }
    }

    companion object {
        @JvmStatic
        fun instance(project: Project): IdeMultiplatformImport {
            return project.extraProperties.getOrPut(IdeMultiplatformImport::class.java.name) {
                IdeMultiplatformImport(project.multiplatformExtension)
            }
        }
    }
}

internal val Project.kotlinIdeMultiplatformImport: IdeMultiplatformImport get() = IdeMultiplatformImport.instance(project)

@ExternalKotlinTargetApi
fun IdeMultiplatformImport.registerExtrasSerializationExtension(
    builder: IdeaExtrasSerializationExtensionBuilder.() -> Unit
) {
    registerExtrasSerializationExtension(IdeaExtrasSerializationExtension(builder))
}
