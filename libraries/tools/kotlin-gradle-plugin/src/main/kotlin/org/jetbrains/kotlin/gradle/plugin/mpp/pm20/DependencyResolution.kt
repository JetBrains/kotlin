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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.getProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.project.model.*
import java.util.ArrayDeque

class GradleModuleDependencyResolver(
    private val projectStructureMetadataModuleBuilder: ProjectStructureMetadataModuleBuilder,
    private val projectModuleBuilder: GradleProjectModuleBuilder
) : ModuleDependencyResolver {

    private fun configurationToResolve(requestingModule: KotlinGradleModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    override fun resolveDependency(requestingModule: KotlinModule, moduleDependency: KotlinModuleDependency): KotlinModule? {
        require(requestingModule is KotlinGradleModule)
        val project = requestingModule.project

        val allComponents = configurationToResolve(requestingModule).incoming.resolutionResult.allComponents
        // TODO: optimize O(n) search, store the resolved components with dependency keys?
        val component = allComponents.find { it.id.matchesModuleDependency(moduleDependency) }
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
                    configurationToResolve(requestingModule),
                    moduleDependency.moduleIdentifier
                ) ?: return null
                val result = projectStructureMetadataModuleBuilder.getModule(component, metadata)
                result
            }
            else -> null
        }
    }
}

// refactor extract to a separate class/interface
// TODO think about multi-variant stub modules for non-Kotlin modules which got more than one chosen variant
internal fun buildSyntheticModule(resolvedComponentResult: ResolvedComponentResult, singleVariantName: String): ExternalSyntheticKotlinModule {
    val moduleDependency = resolvedComponentResult.toModuleDependency()
    return ExternalSyntheticKotlinModule(BasicKotlinModule(moduleDependency.moduleIdentifier).apply {
        BasicKotlinModuleVariant(this@apply, singleVariantName, DefaultLanguageSettingsBuilder()).apply {
            fragments.add(this)
            this.declaredModuleDependencies.addAll(
                resolvedComponentResult.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { it.selected.toModuleDependency() }
            )
        }
    })
}

internal class ExternalSyntheticKotlinModule(private val moduleData: BasicKotlinModule) : KotlinModule by moduleData {
    override fun toString(): String = "synthetic $moduleData"
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
