/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.getProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.jetbrains.kotlin.project.model.*
import java.util.*

class CachingModuleDependencyResolver(private val actualResolver: ModuleDependencyResolver) : ModuleDependencyResolver {
    private val cacheByRequestingModule = WeakHashMap<KotlinModule, MutableMap<KotlinModuleDependency, KotlinModule?>>()

    private fun cacheForRequestingModule(requestingModule: KotlinModule) =
        cacheByRequestingModule.getOrPut(requestingModule) { mutableMapOf() }

    override fun resolveDependency(requestingModule: KotlinModule, moduleDependency: KotlinModuleDependency): KotlinModule? =
        cacheForRequestingModule(requestingModule).getOrPut(moduleDependency) {
            actualResolver.resolveDependency(requestingModule, moduleDependency)
        }
}

open class GradleComponentResultCachingResolver {
    private val cachedResultsByRequestingModule = mutableMapOf<KotlinGradleModule, Map<KotlinModuleIdentifier, ResolvedDependencyResult>>()

    protected open fun configurationToResolve(requestingModule: KotlinGradleModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    protected open fun resolveDependencies(module: KotlinGradleModule): Map<KotlinModuleIdentifier, ResolvedDependencyResult> {
        val allDependencies = configurationToResolve(module).incoming.resolutionResult.allDependencies
        return allDependencies.filterIsInstance<ResolvedDependencyResult>().associateBy { it.toModuleIdentifier() }
    }

    private fun getResultsForModule(module: KotlinGradleModule): Map<KotlinModuleIdentifier, ResolvedDependencyResult> =
        cachedResultsByRequestingModule.getOrPut(module) { resolveDependencies(module) }

    fun resolveModuleDependencyAsDependencyResult(
        requestingModule: KotlinGradleModule,
        moduleDependency: KotlinModuleDependency
    ): ResolvedDependencyResult? =
        getResultsForModule(requestingModule)[moduleDependency.moduleIdentifier]

    companion object {
        fun getForCurrentBuild(project: Project): GradleComponentResultCachingResolver {
            val extraPropertyName = "org.jetbrains.kotlin.dependencyResolution.gradleComponentResolver.${project.getKotlinPluginVersion()}"
            return project.getOrPutRootProjectProperty(extraPropertyName) {
                GradleComponentResultCachingResolver()
            }
        }
    }
}

class GradleModuleDependencyResolver(
    private val gradleComponentResultResolver: GradleComponentResultCachingResolver,
    private val projectStructureMetadataModuleBuilder: ProjectStructureMetadataModuleBuilder,
    private val projectModuleBuilder: GradleProjectModuleBuilder
) : ModuleDependencyResolver {

    override fun resolveDependency(requestingModule: KotlinModule, moduleDependency: KotlinModuleDependency): KotlinModule? {
        require(requestingModule is KotlinGradleModule)
        val project = requestingModule.project

        val dependency = gradleComponentResultResolver.resolveModuleDependencyAsDependencyResult(requestingModule, moduleDependency)
        val id = dependency?.selected?.id

        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild ->
                projectModuleBuilder.buildModulesFromProject(project.project(id.projectPath))
                    .find { it.moduleIdentifier.moduleClassifier == dependency.toModuleIdentifier().moduleClassifier }
            id is ModuleComponentIdentifier -> {
                val metadata = getProjectStructureMetadata(
                    project,
                    dependency,
                    // TODO: consistent choice of configurations across multiple resolvers?
                    configurationToResolveMetadataDependencies(requestingModule.project, requestingModule),
                    dependency.toModuleIdentifier()
                ) ?: return null
                val result = projectStructureMetadataModuleBuilder.getModule(dependency, metadata)
                result
            }
            else -> null
        }
    }

    companion object {
        fun getForCurrentBuild(project: Project): ModuleDependencyResolver {
            val extraPropertyName = "org.jetbrains.kotlin.dependencyResolution.moduleResolver.${project.getKotlinPluginVersion()}"
            return project.getOrPutRootProjectProperty(extraPropertyName) {
                val componentResultResolver = GradleComponentResultCachingResolver.getForCurrentBuild(project)
                val metadataModuleBuilder = ProjectStructureMetadataModuleBuilder()
                val projectModuleBuilder = GradleProjectModuleBuilder(true)
                CachingModuleDependencyResolver(
                    GradleModuleDependencyResolver(componentResultResolver, metadataModuleBuilder, projectModuleBuilder)
                )
            }
        }
    }
}

// refactor extract to a separate class/interface
// TODO think about multi-variant stub modules for non-Kotlin modules which got more than one chosen variant
internal fun buildSyntheticPlainModule(
    resolvedDependencyResult: ResolvedDependencyResult,
    singleVariantName: String,
    project: Project
): ExternalPlainKotlinModule {
    val moduleDependency = resolvedDependencyResult.toModuleDependency()
    return ExternalPlainKotlinModule(BasicKotlinModule(moduleDependency.moduleIdentifier).apply {
        BasicKotlinModuleVariant(this@apply, singleVariantName, DefaultLanguageSettingsBuilder(project)).apply {
            fragments.add(this)
            this.declaredModuleDependencies.addAll(resolvedDependencyResult.resolvedDependencies.map { it.toModuleDependency() })
        }
    })
}

internal class ExternalPlainKotlinModule(private val moduleData: BasicKotlinModule) : KotlinModule by moduleData {
    override fun toString(): String = "external plain $moduleData"

    val singleVariant: KotlinModuleVariant
        get() = moduleData.variants.singleOrNull()
            ?: error("synthetic $moduleData was expected to have a single variant, got: ${moduleData.variants}")
}

internal class ExternalImportedKotlinModule(
    private val moduleData: BasicKotlinModule,
    val projectStructureMetadata: KotlinProjectStructureMetadata,
    val hostSpecificFragments: Set<KotlinModuleFragment>
) : KotlinModule by moduleData {
    val hasLegacyMetadataModule = !projectStructureMetadata.isPublishedAsRoot

    override fun toString(): String = "imported $moduleData"
}

internal const val KOTLIN_AUXILIARY_MODULE_CAPABILITY_INFIX_PART = ".kotlin.auxiliary.module."

internal fun moduleClassifierFromCapabilities(capabilities: Iterable<Capability>): String? {
    val classifierCapability = capabilities.firstOrNull { it.name.contains(KOTLIN_AUXILIARY_MODULE_CAPABILITY_INFIX_PART) }
    // TODO: handle the erroneous case when there's more than one such capability?
    return classifierCapability?.name?.substringAfterLast(KOTLIN_AUXILIARY_MODULE_CAPABILITY_INFIX_PART)
}

internal fun ResolvedDependencyResult.toModuleIdentifier(): KotlinModuleIdentifier = requested.toModuleIdentifier()

internal fun ComponentSelector.toModuleIdentifier(): KotlinModuleIdentifier {
    val moduleClassifier = moduleClassifierFromCapabilities(requestedCapabilities)
    return when (this) {
        is ProjectComponentSelector -> LocalModuleIdentifier(buildName, projectPath, moduleClassifier)
        is ModuleComponentSelector -> MavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, moduleClassifier)
        else -> error("unexpected component selector")
    }
}

internal fun ResolvedDependencyResult.toModuleDependency(): KotlinModuleDependency = KotlinModuleDependency(toModuleIdentifier())

internal fun ComponentSelector.toModuleDependency(): KotlinModuleDependency {
    val moduleId = toModuleIdentifier()
    return KotlinModuleDependency(moduleId)
}