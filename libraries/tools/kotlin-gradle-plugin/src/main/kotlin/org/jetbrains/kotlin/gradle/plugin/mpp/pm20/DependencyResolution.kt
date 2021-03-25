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

class GradleProjectDependencyDiscovery(
    private val project: Project,
    private val variantDependencyDiscovery: VariantDependencyDiscovery,
    private val fragmentDependenciesDiscovery: FragmentDependenciesDiscovery
) : DependencyDiscovery {
    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<KotlinModuleDependency> {
        require(fragment.containingModule.representsProject(project))
        return when (fragment) {
            is KotlinModuleVariant -> variantDependencyDiscovery.discoverDependencies(fragment)
            else -> fragmentDependenciesDiscovery.discoverDependencies(fragment)
        }
    }
}

class FragmentDependenciesDiscovery(
    private val project: Project,
    private val moduleResolver: ModuleDependencyResolver,
    private val fragmentsResolver: ModuleFragmentsResolver
) : DependencyDiscovery {
    private val dependencyScopes = listOf(KotlinDependencyScope.API_SCOPE, KotlinDependencyScope.IMPLEMENTATION_SCOPE)

    private fun KotlinModuleFragment.toSourceSet(): KotlinSourceSet = project.kotlinExtension.sourceSets.getByName(fragmentName)

    // TODO: use the dependency graph resolution implementation?
    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<KotlinModuleDependency> {
        require(fragment.containingModule.representsProject(project))

        val fragmentsWhoseDependenciesAreVisible = fragment.refinesClosure

        // FIXME use dependencyScopes, not just sets of declared module dependencies
        val requestedDependencies = fragmentsWhoseDependenciesAreVisible.flatMap { it.declaredModuleDependencies }.toSet()

        val configurationToResolve = resolvableMetadataConfiguration(
            project,
            fragmentsWhoseDependenciesAreVisible.map { it.toSourceSet() },
            KotlinDependencyScope.compileScopes
        )
        val allComponents = configurationToResolve.incoming.resolutionResult.allComponents

        val componentsByRequestedDependency =
            allComponents.flatMap { component -> component.dependents.map { it to component } }.toMap()
                .mapKeys { it.key.requested.toModuleDependency() }

        val resolvedComponentQueue = ArrayDeque<ResolvedComponentResult>().apply {
            addAll(componentsByRequestedDependency.filterKeys { it in requestedDependencies }.values)
        }

        val visited = mutableSetOf<ResolvedComponentResult>()
        val excludeLegacyMetadataModulesFromResult = mutableSetOf<ResolvedComponentResult>()

        while (resolvedComponentQueue.isNotEmpty()) {
            val component = resolvedComponentQueue.removeFirst()
            visited.add(component)

            val module = moduleResolver.resolveDependency(fragment.containingModule, component.toModuleDependency())
                .takeIf { component !in excludeLegacyMetadataModulesFromResult }
                ?: buildSyntheticModule(component, component.variants.singleOrNull()?.displayName ?: "default")

            val transitiveDepsToVisit = when (val fragmentsResolution = fragmentsResolver.getChosenFragments(fragment, module)) {
                is FragmentResolution.ChosenFragments ->
                    fragmentsResolution.visibleFragments.flatMap { it.declaredModuleDependencies }.mapNotNull { dependency ->
                        componentsByRequestedDependency[dependency]
                    }
                else -> emptyList()
            }

            val newTransitiveDeps = transitiveDepsToVisit.filterTo(mutableListOf()) { it !in visited }

            // With legacy publication scheme, the root MPP module may have a single 'dependency' (available-at) to the metadata module
            val isMetadataModulePublishedSeparately =
                (module is ExternalImportedKotlinModule) && module.hasLegacyMetadataModule
            val singleDependencyComponentOrNull = (component.dependencies.singleOrNull() as? ResolvedDependencyResult)?.selected
            if (isMetadataModulePublishedSeparately && singleDependencyComponentOrNull != null) {
                newTransitiveDeps.add(singleDependencyComponentOrNull)
                excludeLegacyMetadataModulesFromResult.add(singleDependencyComponentOrNull)
            }

            resolvedComponentQueue.addAll(newTransitiveDeps)
        }

        return (visited - excludeLegacyMetadataModulesFromResult).map { it.toModuleDependency() }
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

class VariantDependencyDiscovery(
    val project: Project
) : DependencyDiscovery {

    private val kotlinExtension
        get() = project.kotlinExtension

    private fun resolvedComponentResults(fragment: KotlinModuleFragment): Iterable<ResolvedComponentResult> {
        val compilation = kotlinExtension.targets.asSequence()
            .flatMap { it.compilations.asSequence() }
            .find { it.defaultSourceSetName == fragment.fragmentName }
        requireNotNull(compilation)

        // TODO distinguish between compile and runtime dependencies?
        val configuration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
        return configuration.incoming.resolutionResult.allComponents
    }

    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<KotlinModuleDependency> {
        require(fragment is KotlinModuleVariant)
        require(fragment.containingModule.representsProject(project))

        val components = resolvedComponentResults(fragment)
        return components.flatMap { component ->
            val classifiers = moduleClassifiersFromCapabilities(component.variants.flatMap { it.capabilities })
            when (val id = component.id) {
                is ProjectComponentIdentifier -> {
                    classifiers.map { LocalModuleIdentifier(id.build.name, id.projectPath, it) }
                }
                is ModuleComponentIdentifier -> {
                    classifiers.map { id.toSingleModuleIdentifier(it) }
                }
                else -> return@flatMap emptyList<KotlinModuleIdentifier>() // TODO check that no other options are possible, throw errors?
            }
        }.map(::KotlinModuleDependency)
    }
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
