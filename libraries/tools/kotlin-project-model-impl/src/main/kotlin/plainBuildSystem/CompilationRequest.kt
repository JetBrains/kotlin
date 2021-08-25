/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.kotlin.project.modelx.FragmentId
import org.jetbrains.kotlin.project.modelx.KotlinModule
import org.jetbrains.kotlin.project.modelx.ModuleId

@Serializable
data class CompilationRequest(
    val moduleId: ModuleId,
    val fragments: List<FragmentConfig>,
    val libs: List<String>,
) {
    val fragmentMap by lazy { fragments.associateBy(FragmentConfig::id) }
}

@Serializable
data class FragmentConfig(
    val id: FragmentId,
    val refines: List<FragmentId> = emptyList(),
    val settings: Map<String, JsonElement> = emptyMap(),
    // val freeArgs: List<String> = emptyList(),
    val attributes: Map<String, JsonElement>? = null,
    val moduleDependencies: List<ModuleId> = emptyList()
)

@Serializable
data class DependencyResolutionConfig(
    val modules: Map<ModuleId, DependencyModuleConfig>
)

@Serializable
data class DependencyModuleConfig(
    val id: ModuleId,
    val hasKpmMetadata: Boolean,
    val metaArtifact: String?,
    val variants: Map<String, VariantConfig>,
) {
    @Serializable
    data class VariantConfig(
        val artifact: String,
        val dependencies: List<String> = emptyList(),
        val attributes: Map<String, JsonElement> = emptyMap()
    )
}

fun parseCompilationRequest(content: String) = Json.decodeFromString(CompilationRequest.serializer(), content)
