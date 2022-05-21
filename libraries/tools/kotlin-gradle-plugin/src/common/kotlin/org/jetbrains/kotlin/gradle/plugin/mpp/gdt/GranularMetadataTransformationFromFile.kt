/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.gdt

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.gdt.CacheableMetadataDependencyResolution.*
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import java.io.File
import java.util.*
import java.util.ArrayDeque

internal class ModuleIds(
    val projectsByPath: Map<String, CacheableProject>
) {
    fun fromComponentSelector(
        componentSelector: ComponentSelector
    ): ModuleDependencyIdentifier = when (componentSelector) {
        is ProjectComponentSelector -> idOfRootModuleByProjectPath(componentSelector.projectPath)
        is ModuleComponentSelector -> ModuleDependencyIdentifier(componentSelector.group, componentSelector.module)
        else -> idFromName(componentSelector.displayName)
    }

    fun fromDependency(dependency: CacheableDependency): ModuleDependencyIdentifier = when (dependency) {
        is CacheableDependency.Project -> idOfRootModule(dependency.project)
        is CacheableDependency.External -> ModuleDependencyIdentifier(dependency.group, dependency.name)
    }

    fun fromComponent(component: ResolvedComponentResult) =
        if ((component.id as? ProjectComponentIdentifier)?.build?.isCurrentBuild == false)
            ModuleDependencyIdentifier(component.moduleVersion?.group ?: "unspecified", component.moduleVersion?.name ?: "unspecified")
        else
            fromComponentId(component.id)

    private fun fromComponentId(
        componentIdentifier: ComponentIdentifier
    ): ModuleDependencyIdentifier =
        when (componentIdentifier) {
            is ProjectComponentIdentifier -> idOfRootModuleByProjectPath(
                componentIdentifier.projectPath
            )
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentIdentifier.group, componentIdentifier.module)
            else -> idFromName(componentIdentifier.displayName)
        }


    fun idOfRootModule(project: CacheableProject): ModuleDependencyIdentifier =
        ModuleDependencyIdentifier(project.group, project.name)

    fun idOfRootModuleByProjectPath(path: String) = projectsByPath[path]
        ?.let { idOfRootModule(it) }
        ?: error("Invalid path $path")

    fun idFromName(name: String) = ModuleDependencyIdentifier(null, name)
}

internal val ResolvedComponentResult.allDependencies: Set<DependencyResult> get() {
    val visited = mutableSetOf<ResolvedComponentResult>()
    val result = mutableSetOf<DependencyResult>()
    fun ResolvedComponentResult.walk() {
        if (!visited.add(this)) return
        for (dependency in dependencies) {
            result.add(dependency)
            if (dependency is ResolvedDependencyResult) {
                dependency.selected.walk()
            }
        }
    }

    this.walk()
    return result
}

internal val ResolvedComponentResult.allComponents: Set<ResolvedComponentResult> get() {
    val visited = mutableSetOf<ResolvedComponentResult>()
    fun ResolvedComponentResult.walk() {
        if (!visited.add(this)) return
        for (dependency in dependencies) {
            if (dependency is ResolvedDependencyResult) {
                dependency.selected.walk()
            }
        }
    }

    this.walk()
    return visited
}