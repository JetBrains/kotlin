/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainMode
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.requestDefaultNodeJs
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.KotlinIsolatedNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.KotlinAggregatedNpmWorkspaceService
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled
import org.jetbrains.kotlin.gradle.tasks.registerTask

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

    if (project.isIsolatedNpmResolutionEnabled) {
        project.declareAllProjectsAsSharedNpmDependencies(platform)
    }

    val setupSharedNpmProjectTask = project.registerTask<KotlinSetupSharedNpmProjectTask>(platform.setupSharedNpmProjectTaskName) { task ->
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

    // The npm dependencies of the whole build are installed into a single shared npm root project,
    // so they are installed only once, for the JS platform.
    if (project.isIsolatedNpmResolutionEnabled && platform == JsPlatformDisambiguator) {
        val toolchainDisabled = project.kotlinPropertiesProvider.nodeJsToolchainMode == NodeJsToolchainMode.DISABLE

        // The Node.js toolchain is not necessarily enabled, in which case Node.js is provisioned
        // by this project itself, exactly like for the test and run tasks of a JS target.
        val nodeEnvSpec = if (toolchainDisabled) NodeJsPlugin.apply(project) else null
        val nodeExecutable: Provider<String> = if (nodeEnvSpec != null) {
            nodeEnvSpec.executable
        } else {
            val nodeJsRequest = project.requestDefaultNodeJs()
            NodeJsToolchainService.registerIfAbsent(project)
                .flatMap { toolchain -> nodeJsRequest.flatMap { request -> toolchain.request(request) } }
                .flatMap { it.executable }
        }

        project.registerTask<KotlinIsolatedNpmInstallTask>(KotlinIsolatedNpmInstallTask.NAME) { task ->
            task.description = "Installs the npm dependencies of the '$rootDirectoryName' shared npm project"
            task.sharedNpmProjectDirectory.set(setupSharedNpmProjectTask.flatMap { it.outputDirectory })
            task.nodeExecutable.set(nodeExecutable)
            if (nodeEnvSpec != null) {
                with(nodeEnvSpec) {
                    task.dependsOn(project.nodeJsSetupTaskProvider)
                }
            }
            // Matches the default of `org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension.ignoreScripts`.
            task.ignoreScripts.set(true)
        }

        // The project that assembles the shared npm project does not necessarily have a Kotlin target,
        // so the tasks that use the service are wired here as well.
        KotlinAggregatedNpmWorkspaceService.useFromTasks(project)
    }
}

/**
 * Declares every project of the build as a contributor to the shared npm project.
 *
 * A project cannot be inspected from another project when Isolated Projects are enabled,
 * so only the isolated view of each project is used, which exposes just its path.
 */
private fun Project.declareAllProjectsAsSharedNpmDependencies(platform: HasPlatformDisambiguator) {
    val configurationName = platform.npmSharedDependenciesConfigurationName
    gradle.allprojects { contributor ->
        dependencies.add(
            configurationName,
            dependencies.project(contributor.isolated.path),
        )
    }
}
