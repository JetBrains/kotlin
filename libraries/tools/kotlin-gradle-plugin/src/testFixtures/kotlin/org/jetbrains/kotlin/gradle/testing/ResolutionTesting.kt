/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
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
): Map<ComponentPath, ResolvedComponentWithArtifacts> {
    val selfProjectPath = incoming.resolutionResult.root.variants.single().owner.projectPathOrNull
    val artifacts = resolveProjectDependencyVariantsFromArtifacts(removeAttributesNamed).filterNot { it.path == selfProjectPath }
    val components = resolveProjectDependencyComponents().filterNot { it.path == selfProjectPath }
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

fun KotlinTarget.compilationResolution(compilationName: String = "main") = compilations
    .getByName(compilationName).internal.configurations.compileDependencyConfiguration
    .resolveProjectDependencyComponentsWithArtifacts()

private data class ResolvedVariant(
    val path: String,
    val attributes: Map<String, String>,
)

private data class ResolvedComponent(
    val path: String,
    val configuration: String,
)

private fun Configuration.resolveProjectDependencyVariantsFromArtifacts(
    removeAttributesNamed: Set<String>
): List<ResolvedVariant> {
    return incoming.artifacts.artifacts
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

private fun Configuration.resolveProjectDependencyComponents(): List<ResolvedComponent> {
    return incoming.resolutionResult.allComponents
        .map { component ->
            ResolvedComponent(
                component.id.projectPathOrNull ?: component.id.displayName,
                // Expect a single variant to always be selected?
                component.variants.single().displayName
            )
        }
}
