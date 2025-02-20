/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull

data class ResolvedComponentWithArtifacts(
    val configuration: String,
    val artifacts: MutableList<Map<String, String>> = mutableListOf(),
) : java.io.Serializable

fun Configuration.resolveProjectDependencyComponentsWithArtifacts(): Map<String, ResolvedComponentWithArtifacts> {
    val artifacts = resolveProjectDependencyVariantsFromArtifacts().filterNot { it.path == ":" }
    val components = resolveProjectDependencyComponents().filterNot { it.path == ":" }
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

private fun Configuration.resolveProjectDependencyVariantsFromArtifacts(): List<ResolvedVariant> {
    return incoming.artifacts.artifacts
        .map { artifact ->
            val uklibAttributes: List<Attribute<*>> = artifact.variant.attributes.keySet()
                .sortedBy { it.name }
            ResolvedVariant(
                artifact.variant.owner.projectPathOrNull ?: artifact.variant.owner.displayName,
                uklibAttributes.associateBy({ it }) {
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