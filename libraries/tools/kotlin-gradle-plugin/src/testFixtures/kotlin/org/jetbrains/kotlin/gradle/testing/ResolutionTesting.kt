/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull

data class ResolvedComponentWithArtifacts(
    val configuration: String,
    val artifacts: MutableList<Map<String, String>> = mutableListOf(),
) : java.io.Serializable

typealias ComponentPath = String

fun Configuration.resolveProjectDependencyComponentsWithArtifacts(
    removeAttributesNamed: Set<String> = setOf(
        // The status is inferred from the presence of "-SNAPSHOT" in this version. It should have no effect on resolution, but makes testing against snapshot versions painful
        "org.gradle.status",
    ),
): Map<ComponentPath, ResolvedComponentWithArtifacts> = resolveProjectDependencyComponentsWithArtifacts(
    resolvedArtifacts = incoming.artifacts.artifacts,
    allComponentsProvider = incoming.resolutionResult.allComponents,
    rootComponentProvider = incoming.resolutionResult.root,
    removeAttributesNamed = removeAttributesNamed,
)

fun Configuration.resolveProjectDependencyComponentsWithArtifactsProvider(
    removeAttributesNamed: Set<String> = setOf(
        "org.gradle.status",
    ),
): Provider<Map<ComponentPath, ResolvedComponentWithArtifacts>> {
    val componentsProvider = incoming.resolutionResult.rootComponent.map { incoming.resolutionResult.allComponents }
    val artifactsProvider = incoming.artifacts.resolvedArtifacts
    val rootComponentProvider = incoming.resolutionResult.rootComponent
    return rootComponentProvider.flatMap { root ->
        componentsProvider.flatMap { components ->
            artifactsProvider.map { artifacts ->
                resolveProjectDependencyComponentsWithArtifacts(
                    resolvedArtifacts = artifacts,
                    allComponentsProvider = components,
                    rootComponentProvider = root,
                    removeAttributesNamed = removeAttributesNamed,
                )
            }
        }
    }
}

fun resolveProjectDependencyComponentsWithArtifacts(
    resolvedArtifacts: Set<ResolvedArtifactResult>,
    allComponentsProvider: Set<ResolvedComponentResult>,
    rootComponentProvider: ResolvedComponentResult,
    removeAttributesNamed: Set<String> = setOf(
        // The status is inferred from the presence of "-SNAPSHOT" in this version. It should have no effect on resolution, but makes testing against snapshot versions painful
        "org.gradle.status",
    ),
): Map<ComponentPath, ResolvedComponentWithArtifacts> {
    val selfProjectPath = rootComponentProvider.variants.single().owner.projectPathOrNull
    val artifacts = resolveProjectDependencyVariantsFromArtifacts(
        removeAttributesNamed = removeAttributesNamed,
        resolvedArtifacts = resolvedArtifacts,
    ).filterNot { it.path == selfProjectPath }
    val components = resolveProjectDependencyComponents(
        allComponentsProvider = allComponentsProvider
    ).filterNot { it.path == selfProjectPath }
    val componentToArtifacts = LinkedHashMap<String, ResolvedComponentWithArtifacts>()
    components.forEach { component ->
        if (componentToArtifacts[component.path] == null) {
            componentToArtifacts[component.path] = ResolvedComponentWithArtifacts(component.configuration)
        } else {
            error("${component} resolved multiple times?")
        }
    }
    artifacts.forEach { artifact ->
        componentToArtifacts[artifact.path]?.let {
            it.artifacts.add(artifact.attributes)
        } ?: error("Missing resolved component for artifact: ${artifact}")
    }
    return componentToArtifacts
}

fun KotlinTarget.compilationResolution(compilationName: String = "main"): Map<ComponentPath, ResolvedComponentWithArtifacts> {
    return compilationConfiguration(compilationName).resolveProjectDependencyComponentsWithArtifacts()
}
fun KotlinTarget.runtimeResolution(compilationName: String = "main"): Map<ComponentPath, ResolvedComponentWithArtifacts> {
    return runtimeConfiguration(compilationName).resolveProjectDependencyComponentsWithArtifacts()
}

fun KotlinTarget.compilationConfiguration(compilationName: String = "main"): Configuration {
    // workaround for KT-76284
    val compilation = compilations
        .getByName(compilationName)
    return compilation.internal.configurations.compileDependencyConfiguration
}
fun KotlinTarget.runtimeConfiguration(compilationName: String = "main"): Configuration {
    val compilation = compilations
        .getByName(compilationName)
    return compilation.internal.configurations.runtimeDependencyConfiguration ?: error("Missing runtime configuration in $compilation")
}

private data class ResolvedVariant(
    val path: String,
    val attributes: Map<String, String>,
)

private data class ResolvedComponent(
    val path: String,
    val configuration: String,
)

private fun resolveProjectDependencyVariantsFromArtifacts(
    removeAttributesNamed: Set<String>,
    resolvedArtifacts: Set<ResolvedArtifactResult>
): List<ResolvedVariant> {
    return resolvedArtifacts
        .map { artifact ->
            val attributes: List<Attribute<*>> = artifact.variant.attributes.keySet()
                .filterNot { it.name in removeAttributesNamed }
                .sortedBy { it.name }
            ResolvedVariant(
                artifact.variant.owner.projectPathOrNull ?: artifact.variant.owner.displayName,
                attributes.associateBy({ it }) {
                    artifact.variant.attributes.getAttribute(it).toString()
                }.mapKeys { it.key.name }
            )
        }
}

private fun resolveProjectDependencyComponents(
    allComponentsProvider: Set<ResolvedComponentResult>,
): List<ResolvedComponent> {
    return allComponentsProvider
        .map { component ->
            ResolvedComponent(
                component.id.projectPathOrNull ?: component.id.displayName,
                // Expect a single variant to always be selected?
                component.variants.single().displayName
            )
        }
}
