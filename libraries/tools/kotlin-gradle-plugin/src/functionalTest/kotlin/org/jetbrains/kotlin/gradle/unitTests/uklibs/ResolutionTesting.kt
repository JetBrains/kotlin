/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import org.jetbrains.kotlin.utils.keysToMap

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

fun KotlinTarget.compilationRes() = compilations.getByName("main").internal.configurations.compileDependencyConfiguration
    .resolveProjectDependencyComponentsWithArtifacts()

val uklibTransformationIosArm64Attributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "ios_arm64",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibTransformationJvmAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "jvm",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibTransformationMetadataAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "whole_uklib",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibTransformationJsAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "js_ir",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibTransformationWasmJsAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "wasm_js",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibTransformationWasmWasiAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "wasm_wasi",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val uklibVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.usage" to "kotlin-uklib-api",
)

val jvmPomRuntimeAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.status" to "release",
    "org.gradle.usage" to "java-runtime",
)

val jvmPomApiAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.status" to "release",
    "org.gradle.usage" to "java-api",
)

val platformJvmVariantAttributes = mapOf(
    "artifactType" to "jar",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "standard-jvm",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "java-runtime",
    "org.jetbrains.kotlin.platform.type" to "jvm",
)

val metadataVariantAttributes = mapOf(
    "artifactType" to "jar",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "kotlin-metadata",
    "org.jetbrains.kotlin.platform.type" to "common",
)

val releaseStatus = mapOf(
    "org.gradle.status" to "release",
)

// We only emit packing in secondary variants which are not published?
val nonPacked = mapOf(
    "org.jetbrains.kotlin.klib.packaging" to "non-packed",
)

val jarArtifact = mapOf(
    "artifactType" to "jar",
)

val uklibArtifact = mapOf(
    "artifactType" to "uklib",
)

val platformIosArm64Attributes = mapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.native.target" to "ios_arm64",
    "org.jetbrains.kotlin.platform.type" to "native",
)

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