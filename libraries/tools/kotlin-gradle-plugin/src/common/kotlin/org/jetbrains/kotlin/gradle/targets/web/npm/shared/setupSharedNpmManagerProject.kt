/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator

internal val HasPlatformDisambiguator.setupSharedNpmProjectTaskName: String
    get() = extensionName("setupSharedNpmProject")

/**
 * Sets up the consumer side of the shared-npm-project flow on the root project for one
 * platform, see [org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin].
 */
internal fun setupSharedNpmProject(
    project: Project,
    platform: HasPlatformDisambiguator,
    rootDirectoryName: String,
) {
    val sharedDependenciesResolver = project.createResolvableNpmSharedPackageJsonFilesConfiguration(platform)

    project.registerTask<KotlinSetupSharedNpmProjectTask>(platform.setupSharedNpmProjectTaskName) { task ->
        task.description =
            "Assembles the '$rootDirectoryName' shared npm project from the projects declared on '${platform.npmSharedDependenciesConfigurationName}'"
        task.packageJsonFiles.from(
            sharedDependenciesResolver.incoming.artifactView { it.lenient(true) }.files
        )
        task.rootPackageName.set(project.name)
        task.rootPackageVersion.set(project.version.toString())
        task.outputDirectory.set(project.layout.buildDirectory.dir("$rootDirectoryName/shared-npm-project"))
        task.usesService(project.kotlinToolingDiagnosticsCollectorProvider)
        task.setupKotlinToolingDiagnosticsParameters(project)
    }
}
