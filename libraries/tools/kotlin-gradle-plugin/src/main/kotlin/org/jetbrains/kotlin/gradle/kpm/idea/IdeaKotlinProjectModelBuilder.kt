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

    enum class DependencyResolutionPhase {
        PreDependencyResolution,
        SourceDependencyResolution,
        BinaryDependencyResolution,
        PostDependencyResolution
    }

    enum class DependencyResolutionLevel {
        Default, Overwrite
    }

    enum class DependencyTransformationPhase {
        PreDependencyTransformationPhase,
        FreeDependencyTransformationPhase,
        DependencyFilteringPhase,
        PostDependencyTransformationPhase
    }

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
