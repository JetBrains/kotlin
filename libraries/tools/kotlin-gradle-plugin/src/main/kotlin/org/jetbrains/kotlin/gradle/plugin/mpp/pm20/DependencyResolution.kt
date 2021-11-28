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
    private val cachedResultsByRequestingModule = mutableMapOf<KotlinGradleModule, Map<KotlinModuleIdentifier, ResolvedComponentResult>>()

    protected open fun configurationToResolve(requestingModule: KotlinGradleModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    protected open fun resolveDependencies(module: KotlinGradleModule): Map<KotlinModuleIdentifier, ResolvedComponentResult> {
        val allComponents = configurationToResolve(module).incoming.resolutionResult.allComponents
        // FIXME handle multi-component results
        return allComponents.flatMap { component -> component.toModuleIdentifiers().map { it to component } }.toMap()
    }

    private fun getResultsForModule(module: KotlinGradleModule): Map<KotlinModuleIdentifier, ResolvedComponentResult> =
        cachedResultsByRequestingModule.getOrPut(module) { resolveDependencies(module) }

    fun resolveModuleDependencyAsComponentResult(
        requestingModule: KotlinGradleModule,
        moduleDependency: KotlinModuleDependency
    ): ResolvedComponentResult? =
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

        val component = gradleComponentResultResolver.resolveModuleDependencyAsComponentResult(requestingModule, moduleDependency)
        val id = component?.id

        //FIXME multiple?
        val classifier = moduleClassifiersFromCapabilities(component?.variants?.flatMap { it.capabilities }.orEmpty()).single()

        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild ->
                projectModuleBuilder.buildModulesFromProject(project.project(id.projectPath))
                    .find { it.moduleIdentifier.moduleClassifier == classifier }
            id is ModuleComponentIdentifier -> {
                val metadata = getProjectStructureMetadata(
                    project,
                    component,
                    // TODO: consistent choice of configurations across multiple resolvers?
                    configurationToResolveMetadataDependencies(requestingModule.project, requestingModule),
                    moduleDependency.moduleIdentifier
                ) ?: return null
                val result = projectStructureMetadataModuleBuilder.getModule(component, metadata)
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
    resolvedComponentResult: ResolvedComponentResult,
    singleVariantName: String,
    project: Project
): ExternalPlainKotlinModule {
    val moduleDependency = resolvedComponentResult.toModuleDependency()
    return ExternalPlainKotlinModule(BasicKotlinModule(moduleDependency.moduleIdentifier).apply {
        BasicKotlinModuleVariant(this@apply, singleVariantName, DefaultLanguageSettingsBuilder(project)).apply {
            fragments.add(this)
            this.declaredModuleDependencies.addAll(
                resolvedComponentResult.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { it.selected.toModuleDependency() }
            )
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

private fun ModuleComponentIdentifier.toSingleModuleIdentifier(classifier: String? = null): MavenModuleIdentifier =
    MavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, classifier)

internal fun ComponentIdentifier.matchesModule(module: KotlinModule): Boolean =
    matchesModuleIdentifier(module.moduleIdentifier)

internal fun ResolvedComponentResult.toModuleIdentifiers(): List<KotlinModuleIdentifier> {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    return classifiers.map { moduleClassifier ->
        when (val id = id) {
            is ProjectComponentIdentifier -> LocalModuleIdentifier(id.build.name, id.projectPath, moduleClassifier)
            is ModuleComponentIdentifier -> id.toSingleModuleIdentifier()
            else -> MavenModuleIdentifier(moduleVersion?.group.orEmpty(), moduleVersion?.name.orEmpty(), moduleClassifier)
        }
    }
}

// FIXME this mapping doesn't have enough information to choose auxiliary modules
internal fun ResolvedComponentResult.toSingleModuleIdentifier(): KotlinModuleIdentifier {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    val moduleClassifier = classifiers.single() // FIXME handle multiple capabilities
    return when (val id = id) {
        is ProjectComponentIdentifier -> LocalModuleIdentifier(id.build.name, id.projectPath, moduleClassifier)
        is ModuleComponentIdentifier -> id.toSingleModuleIdentifier()
        else -> MavenModuleIdentifier(moduleVersion?.group.orEmpty(), moduleVersion?.name.orEmpty(), moduleClassifier)
    }
}

internal fun moduleClassifiersFromCapabilities(capabilities: Iterable<Capability>): Iterable<String?> {
    val classifierCapabilities = capabilities.filter { it.name.contains("..") }
    return if (classifierCapabilities.none()) listOf(null) else classifierCapabilities.map { it.name.substringAfterLast("..") /*FIXME invent a more stable scheme*/ }
}

internal fun ComponentSelector.toModuleIdentifiers(): Iterable<KotlinModuleIdentifier> {
    val moduleClassifiers = moduleClassifiersFromCapabilities(requestedCapabilities)
    return when (this) {
        is ProjectComponentSelector -> moduleClassifiers.map { LocalModuleIdentifier(buildName, projectPath, it) }
        is ModuleComponentSelector -> moduleClassifiers.map { MavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, it) }
        else -> error("unexpected component selector")
    }
}

internal fun ResolvedComponentResult.toModuleDependency(): KotlinModuleDependency = KotlinModuleDependency(toSingleModuleIdentifier())
internal fun ComponentSelector.toModuleDependency(): KotlinModuleDependency {
    val moduleId = toModuleIdentifiers().single() // FIXME handle multiple
    return KotlinModuleDependency(moduleId)
}

internal fun ComponentIdentifier.matchesModuleDependency(moduleDependency: KotlinModuleDependency) =
    matchesModuleIdentifier(moduleDependency.moduleIdentifier)

internal fun ComponentIdentifier.matchesModuleIdentifier(id: KotlinModuleIdentifier): Boolean =
    when (id) {
        is LocalModuleIdentifier -> {
            val projectId = this as? ProjectComponentIdentifier
            projectId?.build?.name == id.buildId && projectId.projectPath == id.projectId
        }
        is MavenModuleIdentifier -> {
            val componentId = this as? ModuleComponentIdentifier
            componentId?.toSingleModuleIdentifier() == id
        }
        else -> false
    }

private fun getProjectStructureMetadata(
    project: Project,
    module: ResolvedComponentResult,
    configuration: Configuration,
    moduleIdentifier: KotlinModuleIdentifier? = null
): KotlinProjectStructureMetadata? {
    val extractor = if (moduleIdentifier != null)
        MppDependencyProjectStructureMetadataExtractor.create(project, module, moduleIdentifier, configuration)
    else
        MppDependencyProjectStructureMetadataExtractor.create(project, module, configuration, resolveViaAvailableAt = true)

    return extractor?.getProjectStructureMetadata()
}
