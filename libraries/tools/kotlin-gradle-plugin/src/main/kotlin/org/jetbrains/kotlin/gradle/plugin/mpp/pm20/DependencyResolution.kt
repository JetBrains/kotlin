/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.ProjectStructureMetadataModuleBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.GradleProjectModuleBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.getProjectStructureMetadata
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.project.model.*
import java.util.ArrayDeque

class GradleModuleDependencyResolver(
    private val project: Project,
    private val projectStructureMetadataModuleBuilder: ProjectStructureMetadataModuleBuilder,
    private val projectModuleBuilder: GradleProjectModuleBuilder
) : ModuleDependencyResolver {

    // FIXME this wont work for tests, as their dependencies are currently not in the all compile dependencies configuration
    private val configurationToResolve: Configuration
        get() = project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)

    override fun resolveDependency(moduleDependency: ModuleDependency): KotlinModule? {
        val allComponents = configurationToResolve.incoming.resolutionResult.allComponents
        // TODO: optimize O(n) search, store the resolved components with dependency keys
        val component = allComponents.find { it.id.matchesModuleDependency(moduleDependency) }
        val id = component?.id
        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild ->
                projectModuleBuilder.buildModuleFromProject(project.project(id.projectPath))
            id is ModuleComponentIdentifier -> {
                val metadata = getProjectStructureMetadata(project, component, configurationToResolve) ?: return null
                projectStructureMetadataModuleBuilder.getModule(id.toModuleIdentifier(), metadata)
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
    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<ModuleDependency> {
        require(fragment.containingModule.representsProject(project))
        return when (fragment) {
            is KotlinModuleVariant -> variantDependencyDiscovery.discoverDependencies(fragment)
            else -> fragmentDependenciesDiscovery.discoverDependencies(fragment)
        }
    }
}

class FragmentDependenciesDiscovery(
    private val project: Project,
    private val expansion: InternalDependencyExpansion,
    private val moduleResolver: ModuleDependencyResolver,
    private val fragmentResolver: KotlinModuleFragmentResolver
) : DependencyDiscovery {
    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<ModuleDependency> {
        require(fragment.containingModule.representsProject(project))

        val requested = expansion.expandInternalFragmentDependencies(fragment).visibleFragments().plus(fragment.refinesClosure)
            .flatMap { it.declaredModuleDependencies }
            .toSet()

        // FIXME: test dependencies won't work here
        val allComponents =
            project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME).incoming.resolutionResult.allComponents

        val componentByModuleDependency = allComponents.associateBy { it.toModuleDependency() }

        val resolvedComponentQueue = ArrayDeque<ResolvedComponentResult>().apply {
            addAll(componentByModuleDependency.filterKeys { it in requested }.values)
        }

        val visited = mutableSetOf<ResolvedComponentResult>()

        while (resolvedComponentQueue.isNotEmpty()) {
            val component = resolvedComponentQueue.removeFirst()
            visited.add(component)

            val module = moduleResolver.resolveDependency(component.toModuleDependency())
                ?: buildStubModule(component, component.variants.singleOrNull()?.displayName ?: "default")

            val visibleFragments = fragmentResolver.getChosenFragments(fragment, module)
            val newTransitiveDeps =
                visibleFragments.chosenFragments.flatMap { it.declaredModuleDependencies }.mapNotNull { dependency ->
                    componentByModuleDependency[dependency].takeIf { component -> component !in visited }
                }

            resolvedComponentQueue.addAll(newTransitiveDeps)
        }

        return visited.map { it.toModuleDependency() }
    }

    // refactor extract to a separate class
    // TODO think about multi-variant stub modules for non-Kotlin modules which got more than one chosen variant
    private fun buildStubModule(resolvedComponentResult: ResolvedComponentResult, singleVariantName: String): KotlinModule {
        val moduleDependency = resolvedComponentResult.toModuleDependency()
        return BasicKotlinModule(moduleDependency.moduleIdentifier).apply {
            BasicKotlinModuleVariant(this@apply, singleVariantName).apply {
                fragments.add(this)
                this.declaredModuleDependencies.addAll(
                    resolvedComponentResult.dependencies
                        .filterIsInstance<ResolvedDependencyResult>()
                        .map { it.selected.toModuleDependency() }
                )
            }
        }
    }
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

    override fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<ModuleDependency> {
        require(fragment is KotlinModuleVariant)
        require(fragment.containingModule.representsProject(project))

        val components = resolvedComponentResults(fragment)
        return components.mapNotNull { component ->
            val moduleIdentifier = when (val id = component.id) {
                is ProjectComponentIdentifier -> LocalModuleIdentifier(id.build.name, id.projectPath)
                is ModuleComponentIdentifier -> id.toModuleIdentifier()
                else -> return@mapNotNull null // TODO check that no other options are possible, throw errors?
            }
            ModuleDependency(moduleIdentifier)
        }
    }
}

private fun ModuleComponentIdentifier.toModuleIdentifier(): MavenModuleIdentifier =
    MavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name)

internal fun ComponentIdentifier.matchesModule(module: KotlinModule): Boolean {
    return when (val moduleId = module.moduleIdentifier) {
        is LocalModuleIdentifier -> {
            val projectId = this as? ProjectComponentIdentifier
            projectId?.build?.name == moduleId.buildId && projectId.projectPath == moduleId.projectId
        }
        is MavenModuleIdentifier -> {
            val componentId = this as? ModuleComponentIdentifier
            componentId?.toModuleIdentifier() == moduleId
        }
        else -> false
    }
}

internal fun ResolvedComponentResult.toModuleDependency(): ModuleDependency = ModuleDependency(
    when (val id = id) {
        is ProjectComponentIdentifier -> LocalModuleIdentifier(id.build.name, id.projectPath)
        is ModuleComponentIdentifier -> id.toModuleIdentifier()
        else -> MavenModuleIdentifier(moduleVersion?.group.orEmpty(), moduleVersion?.name.orEmpty())
    }
)

internal fun ComponentIdentifier.matchesModuleDependency(moduleDependency: ModuleDependency) =
    when (val id = moduleDependency.moduleIdentifier) {
        is LocalModuleIdentifier -> {
            val projectId = this as? ProjectComponentIdentifier
            projectId?.build?.name == id.buildId && projectId.projectPath == id.projectId
        }
        is MavenModuleIdentifier -> {
            val componentId = this as? ModuleComponentIdentifier
            componentId?.toModuleIdentifier() == id
        }
        else -> false
    }
