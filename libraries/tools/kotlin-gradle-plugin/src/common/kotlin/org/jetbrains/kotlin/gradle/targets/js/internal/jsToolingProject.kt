/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.web.npm.NpmDependencyCollectionMode
import org.jetbrains.kotlin.gradle.utils.isProjectIsolationEnabled

/**
 * Returns the project to use for JS and WasmJS tooling plugins.
 *
 * Temp workaround for supporting Isolated Projects.
 * It will definitely break things!
 * Only intended for internal use, to prototype Isolated Projects support.
 */
internal fun Project.jsToolingProject(): Project {
    return if (isConfigurationBasedNpmDependencyCollection || (npmSharedProjectsPerProject && isProjectIsolationEnabled)) {
        this
    } else {
        rootProject
    }
}

private val Project.npmSharedProjectsPerProject: Boolean
    get() = PropertiesProvider(project).npmSharedDependenciesProjectMode.orNull == "PER-PROJECT"

/**
 * `true` when transitive npm dependencies come from the `kotlinNpmSharedDependencies`
 * configurations instead of the root project.
 */
private val Project.isConfigurationBasedNpmDependencyCollection: Boolean
    get() = PropertiesProvider(this).npmDependencyCollectionMode.get() == NpmDependencyCollectionMode.ISOLATED_PROJECTS

/**
 * Check if [project] is the project to use for JS and WasmJS tooling plugins.
 *
 * Incompatible with Isolated Projects.
 *
 * Does nothing if [Project.npmSharedProjectsPerProject] is enabled.
 */
internal fun checkIsJsToolingProject(
    project: Project,
    message: () -> String,
) {
    check(project == project.jsToolingProject(), message)
}
