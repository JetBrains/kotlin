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
    val moduleVersion: ModuleVersion?,
    val componentId: ComponentId,
) : Serializable {

    data class ModuleVersion(
        val group: String,
        val name: String,
    ) : Serializable

    data class ComponentId(
        val group: String?,
        val module: String?,
        val projectPath: String?,
        val buildPath: String?,
        val type: Type,
    ) : Serializable {
        enum class Type { PROJECT, MODULE }
    }

    companion object {
        fun from(
            component: ResolvedComponentResult,
            buildIdentifierAccessor: Provider<BuildIdentifierAccessor.Factory>,
        ): KmpModuleIdentifier {
            val moduleVersion = try {
                component.moduleVersion?.let { ModuleVersion(it.group, it.name) }
            } catch (_: Exception) {
                null
            }

            val componentId = when (val id = component.id) {
                is ProjectComponentIdentifier -> ComponentId(
                    group = null,
                    module = null,
                    projectPath = id.projectPath,
                    buildPath = id.build.compatAccessor(buildIdentifierAccessor).buildPath,
                    type = ComponentId.Type.PROJECT
                )
                is ModuleComponentIdentifier -> ComponentId(
                    group = id.group,
                    module = id.module,
                    projectPath = null,
                    buildPath = null,
                    type = ComponentId.Type.MODULE
                )
                else -> error("Unexpected Component Identifier: '$id' of type ${id.javaClass}")
            }

            return KmpModuleIdentifier(moduleVersion, componentId)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KmpModuleIdentifier) return false

        if (moduleVersion != null && other.moduleVersion != null) {
            return moduleVersion == other.moduleVersion
        }

        // fall back to component matching when a module version is not available on any of the compared identifiers
        return componentId == other.componentId
    }

    override fun hashCode(): Int {
        if (moduleVersion != null) {
            return 31 * moduleVersion.group.hashCode() + moduleVersion.name.hashCode()
        }
        return componentId.hashCode()
    }

    override fun toString(): String {
        val modulePart = moduleVersion?.let { "moduleVersion=${it.group}:${it.name}" } ?: "moduleVersion=null"
        val componentPart = when (componentId.type) {
            ComponentId.Type.PROJECT -> "project ${componentId.buildPath}${componentId.projectPath}"
            ComponentId.Type.MODULE -> "module ${componentId.group}:${componentId.module}"
        }
        return "KmpModuleIdentifier($modulePart, $componentPart)"
    }
}
