/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinSoftwareComponent(
    private val project: Project,
    private val name: String,
    private val kotlinTargets: Iterable<KotlinTarget>
) : SoftwareComponentInternal, ComponentWithVariants {

    override fun getUsages(): Set<UsageContext> = emptySet()

    override fun getVariants(): Set<KotlinTargetComponent> =
        kotlinTargets.map { it.component }.toSet()

    override fun getName(): String = name
}

// At the moment all KN artifacts have JAVA_API usage.
// TODO: Replace it with a specific usage
object NativeUsage {
    const val KOTLIN_KLIB = "kotlin-klib"
}

internal class KotlinPlatformUsageContext(
    val kotlinTarget: KotlinTarget,
    private val usage: Usage,
    val dependencyConfigurationName: String,
    private val publishWithGradleMetadata: Boolean
) : UsageContext {
    private val project: Project get() = kotlinTarget.project

    override fun getUsage(): Usage = usage

    override fun getName(): String = kotlinTarget.targetName + when (dependencyConfigurationName) {
        kotlinTarget.apiElementsConfigurationName -> "-api"
        kotlinTarget.runtimeElementsConfigurationName -> "-runtime"
        else -> error("unexpected configuration")
    }

    private val configuration: Configuration
        get() = project.configurations.getByName(dependencyConfigurationName)

    override fun getDependencies(): MutableSet<out ModuleDependency> =
        if (publishWithGradleMetadata)
            configuration.incoming.dependencies.withType(ModuleDependency::class.java)
        else
            rewriteMppDependenciesToTargetModuleDependencies(this, configuration).toMutableSet()

    override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
        configuration.incoming.dependencyConstraints

    override fun getArtifacts(): MutableSet<out PublishArtifact> =
    // TODO Gradle Java plugin does that in a different way; check whether we can improve this
        configuration.artifacts

    override fun getAttributes(): AttributeContainer =
        configuration.outgoing.attributes

    override fun getCapabilities(): Set<Capability> = emptySet()

    override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
}

private fun rewriteMppDependenciesToTargetModuleDependencies(
    context: KotlinPlatformUsageContext,
    configuration: Configuration
): Set<ModuleDependency> = with(context.kotlinTarget.project) {
    val target = context.kotlinTarget
    val moduleDependencies = configuration.incoming.dependencies.withType(ModuleDependency::class.java).ifEmpty { return emptySet() }

    val targetMainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        ?: return moduleDependencies // Android is not yet supported

    val targetCompileDependenciesConfiguration = project.configurations.getByName(
        when (context.dependencyConfigurationName) {
            target.apiElementsConfigurationName -> targetMainCompilation.compileDependencyConfigurationName
            target.runtimeElementsConfigurationName ->
                (targetMainCompilation as KotlinCompilationToRunnableFiles).runtimeDependencyConfigurationName
            else -> error("unexpected configuration")
        }
    )

    val resolvedCompileDependencies by lazy {
        // don't resolve if no project dependencies on MPP projects are found
        targetCompileDependenciesConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.associateBy {
            Triple(it.moduleGroup, it.moduleName, it.moduleVersion)
        }
    }

    moduleDependencies.map { dependency ->
        when (dependency) {
            !is ProjectDependency -> dependency
            else -> {
                val dependencyProject = dependency.dependencyProject
                val dependencyProjectKotlinExtension = dependencyProject.multiplatformExtension
                    ?: return@map dependency

                if (dependencyProjectKotlinExtension.isGradleMetadataAvailable)
                    return@map dependency

                val resolved = resolvedCompileDependencies[Triple(dependency.group, dependency.name, dependency.version)]
                    ?: return@map dependency

                val resolvedToConfiguration = resolved.configuration

                val dependencyTarget = dependencyProjectKotlinExtension.targets.singleOrNull {
                    resolvedToConfiguration in setOf(
                        it.apiElementsConfigurationName,
                        it.runtimeElementsConfigurationName,
                        it.defaultConfigurationName
                    )
                } ?: return@map dependency

                val publicationDelegate = (dependencyTarget.component as KotlinVariant).publicationDelegate

                dependencies.module(
                    listOf(
                        publicationDelegate?.groupId ?: dependency.group,
                        publicationDelegate?.artifactId ?: dependencyTarget.defaultArtifactId,
                        publicationDelegate?.version ?: dependency.version
                    ).joinToString(":")
                ) as ModuleDependency
            }
        }
    }.toSet()
}
