/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.export.internal

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.internal.ProjectByPath
import org.jetbrains.kotlin.gradle.plugin.internal.ProjectDependencyAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactoryProvider
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull

/**
 * Selects a dependency in the Swift Export graph for the overrides from
 * `xcodeIntegration { configure(dependency) { } }`.
 *
 * Matched against the resolved [SwiftExportResolvedComponent], not the requested coordinates, so an override
 * still applies when conflict resolution picks another version.
 */
internal sealed interface SwiftExportDependencySelector {

    /** How this selector is rendered in diagnostics. */
    val displayName: String

    fun matches(component: SwiftExportResolvedComponent): Boolean

    /**
     * Matches a project by its Gradle path.
     *
     * Like the legacy `swiftExport { export(...) }` DSL, this can't tell apart projects with the same path in
     * different included builds.
     */
    data class ProjectPath(val projectPath: String) : SwiftExportDependencySelector {
        override val displayName: String get() = projectPath

        override fun matches(component: SwiftExportResolvedComponent): Boolean =
            component.id.projectPathOrNull == projectPath
    }

    /**
     * Matches an external module by group and name. The version is ignored.
     *
     * A module substituted with a project (an included build, `dependencySubstitution`) selects a
     * [ProjectComponentIdentifier] but keeps the module coordinates, so those are matched too.
     */
    data class Module(val group: String, val name: String) : SwiftExportDependencySelector {
        override val displayName: String get() = "$group:$name"

        override fun matches(component: SwiftExportResolvedComponent): Boolean {
            val id = component.id
            if (id is ModuleComponentIdentifier && id.group == group && id.module == name) return true
            val moduleVersion = component.moduleVersion ?: return false
            return moduleVersion.group == group && moduleVersion.name == name
        }
    }
}

/**
 * Converts the dependency notations accepted by
 * [org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportIntegration.configure] into selectors.
 *
 * Takes the same notations as the legacy `swiftExport { export(...) }` DSL except [Provider]s, which the caller
 * unwraps to keep them lazy.
 */
internal class SwiftExportDependencySelectorFactory(
    private val dependencyHandler: DependencyHandler,
    private val projectDependencyAccessorFactory: Provider<ProjectDependencyAccessor.Factory>,
    private val projectByPath: ProjectByPath,
) {
    fun fromNotation(dependency: Any): SwiftExportDependencySelector {
        if (dependency is Project) return SwiftExportDependencySelector.ProjectPath(dependency.path)
        val created = dependency as? Dependency ?: dependencyHandler.create(dependency)
        return if (created is ProjectDependency) created.pathSelector() else created.moduleSelector()
    }

    private fun ProjectDependency.pathSelector() = SwiftExportDependencySelector.ProjectPath(
        compatAccessor(projectDependencyAccessorFactory, projectByPath).dependencyProject().path
    )

    private fun Dependency.moduleSelector(): SwiftExportDependencySelector.Module {
        val group = group ?: throw InvalidUserDataException(
            "Cannot configure Swift Export for dependency '$name': it has no group, so it cannot be matched " +
                    "against a resolved component. Use full 'group:name' coordinates or a project dependency."
        )
        return SwiftExportDependencySelector.Module(group = group, name = name)
    }
}

internal fun Project.swiftExportDependencySelectorFactory() = SwiftExportDependencySelectorFactory(
    dependencyHandler = dependencies,
    projectDependencyAccessorFactory = variantImplementationFactoryProvider<ProjectDependencyAccessor.Factory>(),
    projectByPath = ::project,
)
