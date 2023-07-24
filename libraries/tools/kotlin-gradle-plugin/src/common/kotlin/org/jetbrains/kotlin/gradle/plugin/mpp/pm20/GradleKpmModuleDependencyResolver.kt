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
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.capabilities.Capability
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.GradleProjectModuleBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ProjectStructureMetadataModuleBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.jetbrains.kotlin.project.model.*

class GradleKpmModuleDependencyResolver(
    private val gradleComponentResultResolver: GradleKpmComponentResultCachingResolver,
    private val projectStructureMetadataModuleBuilder: ProjectStructureMetadataModuleBuilder,
    private val projectModuleBuilder: GradleProjectModuleBuilder,
) : KpmModuleDependencyResolver {

    override fun resolveDependency(requestingModule: KpmModule, moduleDependency: KpmModuleDependency): KpmModule? {
        require(requestingModule is GradleKpmModule)
        val project = requestingModule.project

        val component = gradleComponentResultResolver.resolveModuleDependencyAsComponentResult(requestingModule, moduleDependency)
        val id = component?.id

        //FIXME multiple?
        val classifier = moduleClassifiersFromCapabilities(component?.variants?.flatMap { it.capabilities }.orEmpty()).single()

        @Suppress("DEPRECATION")
        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild ->
                projectModuleBuilder.buildModulesFromProject(project.project(id.projectPath))
                    .find { it.moduleIdentifier.moduleClassifier == classifier }
            id is ModuleComponentIdentifier -> {
                val metadata = getProjectStructureMetadata(
                    project,
                    component,
                    // TODO: consistent choice of configurations across multiple resolvers?
                    configurationToResolveMetadataDependencies(requestingModule),
                    moduleDependency.moduleIdentifier
                ) ?: return null
                val result = projectStructureMetadataModuleBuilder.getModule(component, metadata)
                result
            }
            else -> null
        }
    }

    companion object {
        fun getForCurrentBuild(project: Project): KpmModuleDependencyResolver {
            val extraPropertyName = "org.jetbrains.kotlin.dependencyResolution.moduleResolver.${project.getKotlinPluginVersion()}"
            return project.getOrPutRootProjectProperty(extraPropertyName) {
                val componentResultResolver = GradleKpmComponentResultCachingResolver.getForCurrentBuild(project)
                val metadataModuleBuilder = ProjectStructureMetadataModuleBuilder()
                val projectModuleBuilder = GradleProjectModuleBuilder(true)
                GradleKpmCachingModuleDependencyResolver(
                    GradleKpmModuleDependencyResolver(componentResultResolver, metadataModuleBuilder, projectModuleBuilder)
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
): GradleKpmExternalPlainModule {
    val moduleDependency = resolvedComponentResult.toKpmModuleDependency()
    return GradleKpmExternalPlainModule(KpmBasicModule(moduleDependency.moduleIdentifier).apply {
        KpmBasicVariant(this@apply, singleVariantName, DefaultLanguageSettingsBuilder(TODO())).apply {
            fragments.add(this)
            this.declaredModuleDependencies.addAll(
                resolvedComponentResult.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { it.selected.toKpmModuleDependency() }
            )
        }
    })
}

internal class GradleKpmExternalPlainModule(private val moduleData: KpmBasicModule) : KpmModule by moduleData {
    override fun toString(): String = "external plain $moduleData"

    val singleVariant: KpmVariant
        get() = moduleData.variants.singleOrNull()
            ?: error("synthetic $moduleData was expected to have a single variant, got: ${moduleData.variants}")
}

internal class GradleKpmExternalImportedModule(
    private val moduleData: KpmBasicModule,
    val projectStructureMetadata: KotlinProjectStructureMetadata,
    val hostSpecificFragments: Set<KpmFragment>,
) : KpmModule by moduleData {
    val hasLegacyMetadataModule = !projectStructureMetadata.isPublishedAsRoot

    override fun toString(): String = "imported $moduleData"
}

private fun ModuleComponentIdentifier.toSingleKpmModuleIdentifier(classifier: String? = null): KpmMavenModuleIdentifier =
    KpmMavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, classifier)

internal fun ComponentIdentifier.matchesModule(module: KpmModule): Boolean =
    matchesModuleIdentifier(module.moduleIdentifier)

internal fun ResolvedComponentResult.toKpmModuleIdentifiers(): List<KpmModuleIdentifier> {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    return classifiers.map { moduleClassifier -> toKpmModuleIdentifier(moduleClassifier) }
}

internal fun ResolvedVariantResult.toKpmModuleIdentifiers(): List<KpmModuleIdentifier> {
    val classifiers = moduleClassifiersFromCapabilities(capabilities)
    return classifiers.map { moduleClassifier -> toKpmModuleIdentifier(moduleClassifier) }
}

// FIXME this mapping doesn't have enough information to choose auxiliary modules
internal fun ResolvedComponentResult.toSingleKpmModuleIdentifier(): KpmModuleIdentifier {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    val moduleClassifier = classifiers.single() // FIXME handle multiple capabilities
    return toKpmModuleIdentifier(moduleClassifier)
}

internal fun ResolvedVariantResult.toSingleKpmModuleIdentifier(): KpmModuleIdentifier = toKpmModuleIdentifiers().singleOrNull()
    ?: error("Unexpected amount of KPM Identifiers from '$this'. Only single Module Identifier was expected")

private fun ResolvedComponentResult.toKpmModuleIdentifier(moduleClassifier: String?): KpmModuleIdentifier {
    return when (val id = id) {
        is ProjectComponentIdentifier -> KpmLocalModuleIdentifier(id.build.buildPathCompat, id.projectPath, moduleClassifier)
        is ModuleComponentIdentifier -> id.toSingleKpmModuleIdentifier()
        else -> KpmMavenModuleIdentifier(moduleVersion?.group.orEmpty(), moduleVersion?.name.orEmpty(), moduleClassifier)
    }
}

private fun ResolvedVariantResult.toKpmModuleIdentifier(moduleClassifier: String?): KpmModuleIdentifier {
    return when (val id = owner) {
        is ProjectComponentIdentifier -> KpmLocalModuleIdentifier(id.build.buildPathCompat, id.projectPath, moduleClassifier)
        is ModuleComponentIdentifier -> id.toSingleKpmModuleIdentifier()
        else -> error("Unexpected component identifier '$id' of type ${id.javaClass}")
    }
}

internal fun moduleClassifiersFromCapabilities(capabilities: Iterable<Capability>): Iterable<String?> {
    val classifierCapabilities = capabilities.filter { it.name.contains("..") }
    return if (classifierCapabilities.none()) listOf(null) else classifierCapabilities.map { it.name.substringAfterLast("..") /*FIXME invent a more stable scheme*/ }
}

internal fun ComponentSelector.toKpmModuleIdentifiers(): Iterable<KpmModuleIdentifier> {
    val moduleClassifiers = moduleClassifiersFromCapabilities(requestedCapabilities)
    return when (this) {
        is ProjectComponentSelector -> {
            val buildPath = if (GradleVersion.current() >= GradleVersion.version("8.2")) buildPath
            else @Suppress("DEPRECATION") ":$buildName"
            moduleClassifiers.map { KpmLocalModuleIdentifier(buildPath, projectPath, it) }
        }
        is ModuleComponentSelector -> moduleClassifiers.map { KpmMavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, it) }
        else -> error("unexpected component selector")
    }
}

internal fun ResolvedComponentResult.toKpmModuleDependency(): KpmModuleDependency = KpmModuleDependency(toSingleKpmModuleIdentifier())
internal fun ComponentSelector.toKpmModuleDependency(): KpmModuleDependency {
    val moduleId = toKpmModuleIdentifiers().single() // FIXME handle multiple
    return KpmModuleDependency(moduleId)
}

internal fun ComponentIdentifier.matchesModuleDependency(moduleDependency: KpmModuleDependency) =
    matchesModuleIdentifier(moduleDependency.moduleIdentifier)

internal fun ComponentIdentifier.matchesModuleIdentifier(id: KpmModuleIdentifier): Boolean =
    when (id) {
        is KpmLocalModuleIdentifier -> {
            val projectId = this as? ProjectComponentIdentifier
            projectId?.build?.buildPathCompat == id.buildId && projectId.projectPath == id.projectId
        }
        is KpmMavenModuleIdentifier -> {
            val componentId = this as? ModuleComponentIdentifier
            componentId?.toSingleKpmModuleIdentifier() == id
        }
        else -> false
    }

@Suppress("UNUSED_PARAMETER")
private fun getProjectStructureMetadata(
    project: Project,
    module: ResolvedComponentResult,
    configuration: Configuration,
    moduleIdentifier: KpmModuleIdentifier? = null,
): KotlinProjectStructureMetadata? {
    TODO("Implement project structure metadata extractor for KPM")
}
