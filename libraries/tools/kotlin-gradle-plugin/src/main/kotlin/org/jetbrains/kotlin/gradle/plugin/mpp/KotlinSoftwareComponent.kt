/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.*

open class KotlinSoftwareComponent(
    private val name: String,
    protected val kotlinTargets: Iterable<KotlinTarget>
) : SoftwareComponentInternal, ComponentWithVariants {

    override fun getUsages(): Set<UsageContext> = emptySet()

    override fun getVariants(): Set<KotlinTargetComponent> =
        kotlinTargets.flatMap { it.components }.toSet()

    override fun getName(): String = name

    // This property is declared in the parent type to allow the usages to reference it without forcing the subtypes to load,
    // which is needed for compatibility with older Gradle versions
    var publicationDelegate: MavenPublication? = null
}

class KotlinSoftwareComponentWithCoordinatesAndPublication(name: String, kotlinTargets: Iterable<KotlinTarget>) :
    KotlinSoftwareComponent(name, kotlinTargets), ComponentWithCoordinates {

    override fun getCoordinates(): ModuleVersionIdentifier = getCoordinatesFromPublicationDelegateAndProject(
        publicationDelegate, kotlinTargets.first().project, null
    )
}

// At the moment all KN artifacts have JAVA_API usage.
// TODO: Replace it with a specific usage
object NativeUsage {
    const val KOTLIN_KLIB = "kotlin-klib"
}

interface KotlinUsageContext : UsageContext {
    val compilation: KotlinCompilation<*>
    val dependencyConfigurationName: String
}

class DefaultKotlinUsageContext(
    override val compilation: KotlinCompilation<*>,
    private val usage: Usage,
    override val dependencyConfigurationName: String,
    private val overrideConfigurationArtifacts: Set<PublishArtifact>? = null
) : KotlinUsageContext {

    private val kotlinTarget: KotlinTarget get() = compilation.target
    private val project: Project get() = kotlinTarget.project

    override fun getUsage(): Usage = usage

    override fun getName(): String = kotlinTarget.targetName + when (dependencyConfigurationName) {
        kotlinTarget.apiElementsConfigurationName -> "-api"
        kotlinTarget.runtimeElementsConfigurationName -> "-runtime"
        else -> "-$dependencyConfigurationName" // for Android variants
    }

    private val configuration: Configuration
        get() = project.configurations.getByName(dependencyConfigurationName)

    override fun getDependencies(): MutableSet<out ModuleDependency> =
        configuration.incoming.dependencies.withType(ModuleDependency::class.java)

    override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> =
        configuration.incoming.dependencyConstraints

    override fun getArtifacts(): Set<PublishArtifact> =
        overrideConfigurationArtifacts ?:
        // TODO Gradle Java plugin does that in a different way; check whether we can improve this
        configuration.artifacts

    override fun getAttributes(): AttributeContainer {
        val configurationAttributes = configuration.attributes

        /** TODO Using attributes of a detached configuration is a small and 'conservative' fix for KT-29758, [HierarchyAttributeContainer]
         * being rejected by Gradle 5.2+; we may need to either not filter the attributes, which will lead to
         * [ProjectLocalConfigurations.ATTRIBUTE] being published in the Gradle module metadata, which will potentially complicate our
         * attributes schema migration, or create proper, non-detached configurations for publishing that are separated from the
         * configurations used for project-to-project dependencies
         */
        val result = project.configurations.detachedConfiguration().attributes

        // Capture type parameter T:
        fun <T> copyAttribute(attribute: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
            to.attribute<T>(attribute, from.getAttribute(attribute)!!)
        }

        configurationAttributes.keySet()
            .filter { it != ProjectLocalConfigurations.ATTRIBUTE }
            .forEach { copyAttribute(it, configurationAttributes, result) }

        return result
    }

    override fun getCapabilities(): Set<Capability> = emptySet()

    override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
}

internal fun rewriteDependenciesToActualModuleDependencies(
    usageContext: KotlinUsageContext,
    moduleDependencies: Set<ModuleDependency>
): Map<ModuleDependency, ModuleDependency> {
    val compilation = usageContext.compilation
    val project = compilation.target.project

    val targetDependenciesConfiguration = project.configurations.getByName(
        when (compilation) {
            is KotlinJvmAndroidCompilation -> {
                // TODO handle Android configuration names in a general way once we drop AGP < 3.0.0
                val variantName = compilation.name
                when (usageContext.usage.name) {
                    Usage.JAVA_API -> variantName + "CompileClasspath"
                    Usage.JAVA_RUNTIME_JARS -> variantName + "RuntimeClasspath"
                    else -> error("Unexpected Usage for usage context: ${usageContext.usage}")
                }
            }
            else -> when (usageContext.usage.name) {
                Usage.JAVA_API -> compilation.compileDependencyConfigurationName
                Usage.JAVA_RUNTIME_JARS -> (compilation as KotlinCompilationToRunnableFiles).runtimeDependencyConfigurationName
                else -> error("Unexpected Usage for usage context: ${usageContext.usage}")
            }
        }
    )

    val resolvedDependencies by lazy {
        // don't resolve if no project dependencies on MPP projects are found
        targetDependenciesConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.associateBy {
            Triple(it.moduleGroup, it.moduleName, it.moduleVersion)
        }
    }

    val resolvedModulesByRootModuleCoordinates = targetDependenciesConfiguration
        .allDependencies.withType(ModuleDependency::class.java)
        .associate { dependency ->
            when (dependency) {
                is ProjectDependency -> {
                    val dependencyProject = dependency.dependencyProject
                    val dependencyProjectKotlinExtension = dependencyProject.multiplatformExtension
                        ?: return@associate dependency to dependency

                    val resolved = resolvedDependencies[Triple(dependency.group, dependency.name, dependency.version)]
                        ?: return@associate dependency to dependency

                    val resolvedToConfiguration = resolved.configuration
                    val dependencyTargetComponent: KotlinTargetComponent = run {
                        dependencyProjectKotlinExtension.targets.forEach { target ->
                            target.components.forEach { component ->
                                if (component.findUsageContext(resolvedToConfiguration) != null)
                                    return@run component
                            }
                        }
                        // Failed to find a matching component:
                        return@associate dependency to dependency
                    }

                    val targetModulePublication = (dependencyTargetComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate
                    val rootModulePublication = dependencyProjectKotlinExtension.rootSoftwareComponent.publicationDelegate

                    // During Gradle POM generation, a project dependency is already written as the root module's coordinates. In the
                    // dependencies mapping, map the root module to the target's module:

                    val rootModule = project.dependencies.module(
                        listOf(
                            rootModulePublication?.groupId ?: dependency.group,
                            rootModulePublication?.artifactId ?: dependencyProject.name,
                            rootModulePublication?.version ?: dependency.version
                        ).joinToString(":")
                    ) as ModuleDependency

                    rootModule to project.dependencies.module(
                        listOf(
                            targetModulePublication?.groupId ?: dependency.group,
                            targetModulePublication?.artifactId ?: dependencyTargetComponent.defaultArtifactId,
                            targetModulePublication?.version ?: dependency.version
                        ).joinToString(":")
                    ) as ModuleDependency
                }
                else -> {
                    val resolvedDependency = resolvedDependencies[Triple(dependency.group, dependency.name, dependency.version)]
                        ?: return@associate dependency to dependency

                    if (resolvedDependency.moduleArtifacts.isEmpty() && resolvedDependency.children.size == 1) {
                        // This is a dependency on a module that resolved to another module; map the original dependency to the target module
                        val targetModule = resolvedDependency.children.single()
                        dependency to project.dependencies.module(
                            listOf(
                                targetModule.moduleGroup,
                                targetModule.moduleName,
                                targetModule.moduleVersion
                            ).joinToString(":")
                        ) as ModuleDependency

                    } else {
                        dependency to dependency
                    }
                }
            }
        }.mapKeys { (key, _) -> Triple(key.group, key.name, key.version) }

    return moduleDependencies.associate { dependency ->
        val key = Triple(dependency.group, dependency.name, dependency.version)
        val value = resolvedModulesByRootModuleCoordinates[key] ?: dependency
        dependency to value
    }
}

internal fun KotlinTargetComponent.findUsageContext(configurationName: String): UsageContext? {
    val usageContexts = when (this) {
        is KotlinVariantWithMetadataDependency -> originalUsages
        is SoftwareComponentInternal -> usages
        else -> emptySet()
    }
    return usageContexts.find { usageContext ->
        if (usageContext !is KotlinUsageContext) return@find false
        val compilation = usageContext.compilation
        configurationName in compilation.relatedConfigurationNames ||
                configurationName == compilation.target.apiElementsConfigurationName ||
                configurationName == compilation.target.runtimeElementsConfigurationName ||
                configurationName == compilation.target.defaultConfigurationName
    }
}