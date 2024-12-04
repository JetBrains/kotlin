/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetComponentWithPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.getValue


// TODO(Dmitrii Krasnov): remove it with KT-71454
@Deprecated(
    message = "This implementation is not compatible with isolated projects and will be removed soon.",
    replaceWith = ReplaceWith("PomDependenciesRewriterImpl")
)
internal class DeprecatedPomDependenciesRewriter(
    project: Project,
    component: KotlinTargetComponent,
) : PomDependenciesRewriter() {

    override fun createDependenciesMappingForEachUsageContext(): List<Map<ModuleCoordinates, ModuleCoordinates>> {
        return dependenciesMappingForEachUsageContext
    }

    // Get the dependencies mapping according to the component's UsageContexts:
    private val dependenciesMappingForEachUsageContext by project.provider {
        component.internal.usages.toList().mapNotNull { usage ->
            // When maven scope is not set, we can shortcut immediately here, since no dependencies from that usage context
            // will be present in maven pom, e.g. from sourcesElements
            val mavenScope = usage.mavenScope ?: return@mapNotNull null
            associateDependenciesWithActualModuleDependencies(usage.compilation, mavenScope)
                // We are only interested in dependencies that are mapped to some other dependencies:
                .filter { (from, to) -> Triple(from.group, from.name, from.version) != Triple(to.group, to.name, to.version) }
        }
    }

    private fun associateDependenciesWithActualModuleDependencies(
        compilation: KotlinCompilation<*>,
        mavenScope: MavenScope,
    ): Map<ModuleCoordinates, ModuleCoordinates> {
        val project = compilation.target.project

        val targetDependenciesConfiguration = project.configurations.getByName(
            when (compilation) {
                is KotlinJvmAndroidCompilation -> {
                    // TODO handle Android configuration names in a general way once we drop AGP < 3.0.0
                    val variantName = compilation.name
                    when (mavenScope) {
                        MavenScope.COMPILE -> variantName + "CompileClasspath"
                        MavenScope.RUNTIME -> variantName + "RuntimeClasspath"
                    }
                }
                else -> when (mavenScope) {
                    MavenScope.COMPILE -> compilation.compileDependencyConfigurationName
                    MavenScope.RUNTIME -> compilation.runtimeDependencyConfigurationName ?: return emptyMap()
                }
            }
        )

        val resolvedDependencies: Map<Triple<String?, String, String?>, ResolvedDependency> by lazy {
            // don't resolve if no project dependencies on MPP projects are found
            targetDependenciesConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.associateBy {
                Triple(it.moduleGroup, it.moduleName, it.moduleVersion)
            }
        }

        return targetDependenciesConfiguration
            .allDependencies.withType(ModuleDependency::class.java)
            .associate { dependency ->
                val coordinates = ModuleCoordinates(dependency.group, dependency.name, dependency.version)
                val noMapping = coordinates to coordinates
                when (dependency) {
                    is ProjectDependency -> {
                        if (GradleVersion.current() >= GradleVersion.version("9.0")) {
                            compilation.project.reportDiagnostic(
                                KotlinToolingDiagnostics.NotCompatibleWithGradle9(
                                    "add 'kotlin.kmp.isolated-projects.support=enable' into 'gradle.properties'"
                                )
                            )
                        }
                        @Suppress("DEPRECATION")
                        val dependencyProject = dependency.dependencyProject
                        val dependencyProjectKotlinExtension = dependencyProject.multiplatformExtensionOrNull
                            ?: return@associate noMapping

                        // Non-default publication layouts are not supported for pom rewriting
                        if (!dependencyProject.kotlinPropertiesProvider.createDefaultMultiplatformPublications)
                            return@associate noMapping

                        val resolved = resolvedDependencies[Triple(dependency.group!!, dependency.name, dependency.version!!)]
                            ?: return@associate noMapping

                        val resolvedToConfiguration = resolved.configuration
                        val dependencyTargetComponent: KotlinTargetComponent = run {
                            dependencyProjectKotlinExtension.targets.forEach { target ->
                                target.internal.kotlinComponents.forEach { component ->
                                    if (component.findUsageContext(resolvedToConfiguration) != null)
                                        return@run component
                                }
                            }
                            // Failed to find a matching component:
                            return@associate noMapping
                        }

                        val targetModulePublication =
                            (dependencyTargetComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate
                        val rootModulePublication = dependencyProjectKotlinExtension.rootSoftwareComponent.publicationDelegate

                        // During Gradle POM generation, a project dependency is already written as the root module's coordinates. In the
                        // dependencies mapping, map the root module to the target's module:

                        val rootModule = ModuleCoordinates(
                            rootModulePublication?.groupId ?: dependency.group,
                            rootModulePublication?.artifactId ?: dependencyProject.name,
                            rootModulePublication?.version ?: dependency.version
                        )

                        rootModule to ModuleCoordinates(
                            targetModulePublication?.groupId ?: dependency.group,
                            targetModulePublication?.artifactId ?: dependencyTargetComponent.defaultArtifactId,
                            targetModulePublication?.version ?: dependency.version
                        )
                    }
                    else -> {
                        val resolvedDependency = resolvedDependencies[Triple(dependency.group, dependency.name, dependency.version)]
                            ?: return@associate noMapping

                        // This is a heuristical check for External Variants.
                        // In ResolvedDependency API these dependencies have no artifacts and single children dependency.
                        // That single dependency is an actual variant that contains artifacts and other dependencies.
                        // For example see: `org.jetbrains.kotlinx:kotlinx-coroutines-core` jvmApiElements-published
                        // It has reference to `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` and has no other dependencies nor artifacts.
                        // If dependency was resolved but moduleArtifacts for some reason failed to resolve: it's OK!
                        // It means there are some artifacts that can't be resolved.
                        // For example resolved project dependency to android variant from included build.
                        val moduleArtifacts = runCatching { resolvedDependency.moduleArtifacts }.getOrNull() ?: return@associate noMapping
                        if (moduleArtifacts.isEmpty() && resolvedDependency.children.size == 1) {
                            val targetModule = resolvedDependency.children.single()
                            coordinates to ModuleCoordinates(
                                targetModule.moduleGroup,
                                targetModule.moduleName,
                                targetModule.moduleVersion
                            )

                        } else {
                            noMapping
                        }
                    }
                }
            }
    }

    private fun KotlinTargetComponent.findUsageContext(configurationName: String): UsageContext? {
        val usageContexts = when (this) {
            is SoftwareComponentInternal -> usages
            else -> emptySet()
        }
        return usageContexts.find { usageContext ->
            if (usageContext !is KotlinUsageContext) return@find false
            val compilation = usageContext.compilation
            val outgoingConfigurations = mutableListOf(
                compilation.target.apiElementsConfigurationName,
                compilation.target.runtimeElementsConfigurationName
            )
            if (compilation is KotlinJvmAndroidCompilation) {
                val androidVariant = compilation.androidVariant
                outgoingConfigurations += listOf(
                    "${androidVariant.name}ApiElements",
                    "${androidVariant.name}RuntimeElements",
                )
            }
            configurationName in outgoingConfigurations
        }
    }
}
