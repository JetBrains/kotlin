/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializationExtensionBuilder
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.DependencyResolutionLevel.Default
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.DependencyResolutionLevel.Overwrite
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isSingleKotlinTargetSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isSinglePlatformTypeSourceSet
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

@ExternalKotlinTargetApi
interface IdeMultiplatformImport {

    fun resolveDependencies(sourceSetName: String): Set<IdeaKotlinDependency>

    fun resolveDependencies(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency>

    fun resolveDependenciesSerialized(sourceSetName: String): List<ByteArray>

    /**
     * @param owner: Should implement [HasMutableExtras]. Passing [Any] is fine to make it easier to cross
     *  ClassLoader boundaries. Passing some non [HasMutableExtras] will just return null
     */
    fun resolveExtrasSerialized(owner: Any): ByteArray?

    fun serialize(dependencies: Iterable<IdeaKotlinDependency>): List<ByteArray>

    fun <T : Any> serialize(key: Extras.Key<T>, value: T): ByteArray?

    /**
     * Registers a given [resolver] to run during Gradle import:
     * The given resolver will only run if [constraint] matches and if not overwritten by another
     * resolver registered in the same [phase] and level=[DependencyResolutionLevel.Overwrite]
     */
    @ExternalKotlinTargetApi
    fun registerDependencyResolver(
        resolver: IdeDependencyResolver,
        constraint: SourceSetConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel = Default,
    )

    /**
     * Registers a given [resolver] to run during Gradle import:
     * The given resolver will only run if [constraint] matches and if not overwritten by another
     * resolver registered in the same [phase] and level [AdditionalArtifactResolutionLevel.Overwrite]
     */
    @ExternalKotlinTargetApi
    fun registerAdditionalArtifactResolver(
        resolver: IdeAdditionalArtifactResolver,
        constraint: SourceSetConstraint,
        phase: AdditionalArtifactResolutionPhase,
        level: AdditionalArtifactResolutionLevel = AdditionalArtifactResolutionLevel.Default
    )

    /**
     * Registers a given [transformer] to run during Gradle import:
     * The resolver will only run if the [constraint] matches the SourceSet
     */
    @ExternalKotlinTargetApi
    fun registerDependencyTransformer(
        transformer: IdeDependencyTransformer,
        constraint: SourceSetConstraint,
        phase: DependencyTransformationPhase
    )

    /**
     * Registers a given [effect] to run during Gradle import:
     * The effect will only run for SourceSets matching the given [constraint]
     */
    @ExternalKotlinTargetApi
    fun registerDependencyEffect(
        effect: IdeDependencyEffect,
        constraint: SourceSetConstraint
    )

    /**
     * Registers a [IdeaKotlinExtrasSerializationExtension] for transporting generic/external data into the
     * IDE process using the [Extras]. Entities that implement [HasMutableExtras] which are imported to the IDE
     * (like KotlinTarget, KotlinCompilation and KotlinSourceSet) will automatically retain their attached extras using
     * the serializers registered as [extension].
     *
     * Note 1: In order to access the extras in the IDE during (or after) Gradle import, a similar
     * extension needs to be registered in the IDE plugin to deserialize the payload.
     * Note 2: Extras that do not have a serializer attached will not be transported into the IDE process
     * Note 3: This transport mechanism will only act as transport mechanics of data. Keeping serializers and deserializers compatible
     * shall be handled by the implementers of the [extension]
     */
    @ExternalKotlinTargetApi
    fun registerExtrasSerializationExtension(
        extension: IdeaKotlinExtrasSerializationExtension
    )

    /**
     * Any [IdeDependencyResolver] has to be registered for a given dependency resolution phase in which it participates
     * The resolution phases will be executed in the order of their enum's ordinal.
     */
    @ExternalKotlinTargetApi
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
         * Resolution phase intended to resolve sources.jar and javadoc.jar dependencies (downloading and transforming from repositories)
         */
        SourcesAndDocumentationResolution,

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
    @ExternalKotlinTargetApi
    enum class DependencyResolutionLevel {
        Default, Overwrite
    }

    @ExternalKotlinTargetApi
    enum class AdditionalArtifactResolutionPhase {
        PreAdditionalArtifactResolution,
        SourcesAndDocumentationResolution,
        PostAdditionalArtifactResolution
    }

    @ExternalKotlinTargetApi
    enum class AdditionalArtifactResolutionLevel {
        Default, Overwrite
    }

    /**
     * Any [IdeDependencyResolver] has to be registered for a given transformation phase.
     * The phases will be executed in the order of this enums ordinal.
     */
    @ExternalKotlinTargetApi
    enum class DependencyTransformationPhase {
        /**
         * Generic dependency transformation phase, intended to run a transformation before all other transformers.
         */
        PreDependencyTransformationPhase,

        /**
         * Dependency transformation phase that is entirely free in its transformation type.
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
    @ExternalKotlinTargetApi
    fun interface SourceSetConstraint {
        operator fun invoke(sourceSet: KotlinSourceSet): Boolean

        @ExternalKotlinTargetApi
        companion object {
            /**
             * Indicating that all SourceSets will *always* match this constraint
             */
            val unconstrained = SourceSetConstraint { true }

            /**
             * Will only match SourceSets that will be compiled by the Native compiler
             * Note: this includes shared native SourceSets, sharing code between multiple native targets
             */
            val isNative = SourceSetConstraint { it.isNativeSourceSet.getOrThrow() }

            /**
             * Only matches SourceSets that share code between at least two native targets, but no non-native target
             */
            val isSharedNative = isNative and SourceSetConstraint { sourceSet ->
                sourceSet.internal.compilations.filterIsInstance<KotlinNativeCompilation>()
                    .map { compilation -> compilation.konanTarget }
                    .toSet().size > 1
            }

            /**
             * Matches SourceSets which only have a single [KotlinPlatformType] associated.
             */
            val isSinglePlatformType = SourceSetConstraint { isSinglePlatformTypeSourceSet(it) }

            /**
             * Matches SourceSets which only participate in compilations of a single [KotlinTarget]
             */
            val isSingleKotlinTarget = SourceSetConstraint { isSingleKotlinTargetSourceSet(it) }

            /**
             * Matches SourceSets that do not have any other SourceSets that declare a 'dependsOn' this SourceSet
             */
            val isLeaf = SourceSetConstraint { sourceSet ->
                (sourceSet.project.multiplatformExtensionOrNull ?: return@SourceSetConstraint true).sourceSets
                    .none { otherSourceSet -> sourceSet in otherSourceSet.dependsOn }
            }

            /**
             * Matches SourceSets that share code between compilations with [KotlinPlatformType.jvm] and [KotlinPlatformType.androidJvm]
             * platform types.
             * NB: The external Android target maintained by Google will likely also use the [KotlinPlatformType.jvm]
             * and therefore will not match this constraint.
             */
            val isJvmAndAndroid = SourceSetConstraint { sourceSet ->
                sourceSet.internal.compilations.map { it.platformType }.filter { it != common }.toSet() == setOf(jvm, androidJvm)
            }

            /**
             * Matches SourceSets that only compile for compilations with [KotlinPlatformType.androidJvm]
             * NB: The external Android target maintained by Google will likely also use the [KotlinPlatformType.jvm]
             * and therefore will not match this constraint.
             */
            val isAndroid = SourceSetConstraint { sourceSet ->
                sourceSet.internal.compilations.map { it.platformType }.toSet() == setOf(androidJvm)
            }
        }
    }

    @ExternalKotlinTargetApi
    companion object {
        internal val logger: Logger = Logging.getLogger(IdeMultiplatformImport::class.java)

        @JvmStatic
        fun instance(project: Project): IdeMultiplatformImport {
            return project.extraProperties.getOrPut(IdeMultiplatformImport::class.java.name) {
                IdeMultiplatformImport(project.kotlinExtension)
            }
        }
    }
}

internal val Project.kotlinIdeMultiplatformImport: IdeMultiplatformImport get() = IdeMultiplatformImport.instance(project)

/**
 * Convenience shortcut method for
 * `registerExtrasSerializationExtension(IdeaKotlinExtrasSerializationExtension(builder))`
 * see [IdeMultiplatformImport.registerExtrasSerializationExtension]
 */
@Suppress("unused")
@ExternalKotlinTargetApi
fun IdeMultiplatformImport.registerExtrasSerializationExtension(
    builder: IdeaKotlinExtrasSerializationExtensionBuilder.() -> Unit
) {
    registerExtrasSerializationExtension(IdeaKotlinExtrasSerializationExtension(builder))
}

/**
 * Combines two given [SourceSetConstraint] using a logical 'or':
 * The resulting constraint will match any SourceSet that matches at least one of the specified constraints
 */
@ExternalKotlinTargetApi
infix fun SourceSetConstraint.or(
    other: SourceSetConstraint
) = SourceSetConstraint { sourceSet ->
    this@or(sourceSet) || other(sourceSet)
}

/**
 * Combines two given [SourceSetConstraint] using a logical 'and':
 * The resulting constraint will match only SourceSets that matches both of the specified constraints
 */
@ExternalKotlinTargetApi
infix fun SourceSetConstraint.and(
    other: SourceSetConstraint
) = SourceSetConstraint { sourceSet ->
    this@and(sourceSet) && other(sourceSet)
}

/**
 * Negates a given [SourceSetConstraint]:
 * The resulting constraint will match only SourceSets that would *not* have been matched by the source constraint
 */
@ExternalKotlinTargetApi
operator fun SourceSetConstraint.not() = SourceSetConstraint { sourceSet ->
    this@not(sourceSet).not()
}
