/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdentifierAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import java.io.Serializable


internal data class KmpModuleIdentifier(
    val moduleId: ModuleId?,
    val componentId: ComponentId,
) : Serializable {

    data class ModuleId(
        val group: String,
        val name: String,
    ) : Serializable

    sealed interface ComponentId : Serializable

    data class ModuleComponentId(
        val group: String,
        val name: String,
    ) : ComponentId

    data class ProjectComponentId(
        val projectPath: String,
        val buildPath: String,
    ) : ComponentId

    companion object {
        fun from(
            component: ResolvedComponentResult,
            buildIdentifierAccessor: Provider<BuildIdentifierAccessor.Factory>,
        ): KmpModuleIdentifier {
            val moduleId = try {
                component.moduleVersion?.let { ModuleId(it.group, it.name) }
            } catch (_: Exception) {
                null
            }

            val componentId = when (val id = component.id) {
                is ProjectComponentIdentifier -> ProjectComponentId(
                    projectPath = id.projectPath,
                    buildPath = id.build.compatAccessor(buildIdentifierAccessor).buildPath
                )
                is ModuleComponentIdentifier -> ModuleComponentId(
                    group = id.group,
                    name = id.module
                )
                else -> error("Unexpected Component Identifier: '$id' of type ${id.javaClass}")
            }

            return KmpModuleIdentifier(moduleId, componentId)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KmpModuleIdentifier) return false

        if (moduleId != null && other.moduleId != null) {
            return moduleId == other.moduleId
        }

        // fall back to component matching when a module version is not available on any of the compared identifiers
        return componentId == other.componentId
    }

    override fun hashCode(): Int {
        if (moduleId != null) {
            return 31 * moduleId.group.hashCode() + moduleId.name.hashCode()
        }
        return componentId.hashCode()
    }

    override fun toString(): String {
        val modulePart = moduleId?.let { "moduleVersion=${it.group}:${it.name}" } ?: "moduleVersion=null"
        val componentPart = when (componentId) {
            is ProjectComponentId -> "project ${componentId.buildPath}${componentId.projectPath}"
            is ModuleComponentId -> "module ${componentId.group}:${componentId.name}"
        }
        return "KmpModuleIdentifier($modulePart, $componentPart)"
    }
}
