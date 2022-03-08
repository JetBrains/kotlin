@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModelBuilder.FragmentConstraint
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.native
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.containingVariants
import org.jetbrains.kotlin.project.model.KotlinModuleVariant
import java.io.File

internal interface IdeaKotlinProjectModelBuildingContext {
    val dependencyResolver: IdeaKotlinDependencyResolver

    companion object Empty : IdeaKotlinProjectModelBuildingContext {
        override val dependencyResolver: IdeaKotlinDependencyResolver = IdeaKotlinDependencyResolver.Empty
    }
}

interface IdeaKotlinProjectModelBuilder {
    /**
     * Any [IdeaKotlinDependencyResolver] has to be registered for a given dependency resolution phase in which it participates
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
     * Any [IdeaKotlinDependencyResolver] has to be registered specifying a certain resolution level.
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
     * Any [IdeaKotlinDependencyTransformer] has to be registered for a given transformation phase.
     * The phases will be executed in the order of this enums ordinal.
     */
    enum class DependencyTransformationPhase {
        /**
         * Generic dependency transformation phase, intended to run a transformation before all other transformers.
         */
        PreDependencyTransformationPhase,

        /**
         * Dependency transformation phase that is free entirely free in its transformation type.
         * Note: Adding dependencies to the resolution result might most likely better be modelled as [IdeaKotlinDependencyResolver]
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
     * Used for scoping [IdeaKotlinDependencyResolver], [IdeaKotlinDependencyTransformer] and [IdeaKotlinDependencyEffect]
     */
    fun interface FragmentConstraint {
        operator fun invoke(fragment: KotlinGradleFragment): Boolean

        companion object {
            val unconstrained = FragmentConstraint { true }
            val isVariant = FragmentConstraint { fragment -> fragment is KotlinModuleVariant }
            val isNative = FragmentConstraint { fragment -> fragment.containingVariants.run { any() && all { it.platformType == native } } }
        }
    }

    @ExternalVariantApi
    fun registerDependencyResolver(
        resolver: IdeaKotlinDependencyResolver,
        constraint: FragmentConstraint,
        phase: DependencyResolutionPhase,
        level: DependencyResolutionLevel = DependencyResolutionLevel.Default,
    )

    @ExternalVariantApi
    fun registerDependencyTransformer(
        transformer: IdeaKotlinDependencyTransformer,
        constraint: FragmentConstraint,
        phase: DependencyTransformationPhase
    )

    @ExternalVariantApi
    fun registerDependencyEffect(
        effect: IdeaKotlinDependencyEffect,
        constraint: FragmentConstraint
    )

    fun buildIdeaKotlinProjectModel(): IdeaKotlinProjectModel

    companion object
}

internal fun IdeaKotlinProjectModelBuildingContext.toIdeaKotlinProjectModel(extension: KotlinPm20ProjectExtension): IdeaKotlinProjectModel {
    return IdeaKotlinProjectModelImpl(
        gradlePluginVersion = extension.project.getKotlinPluginVersion(),
        coreLibrariesVersion = extension.coreLibrariesVersion,
        explicitApiModeCliOption = extension.explicitApi?.cliOption,
        kotlinNativeHome = File(extension.project.konanHome).absoluteFile,
        modules = extension.modules.map { module -> toIdeaKotlinModule(module) }
    )
}

infix fun FragmentConstraint.or(
    other: FragmentConstraint
) = FragmentConstraint { fragment ->
    this@or(fragment) || other(fragment)
}

infix fun FragmentConstraint.and(
    other: FragmentConstraint
): FragmentConstraint = FragmentConstraint { fragment ->
    this@and(fragment) && other(fragment)
}

operator fun FragmentConstraint.not() = FragmentConstraint { fragment ->
    this@not(fragment).not()
}
