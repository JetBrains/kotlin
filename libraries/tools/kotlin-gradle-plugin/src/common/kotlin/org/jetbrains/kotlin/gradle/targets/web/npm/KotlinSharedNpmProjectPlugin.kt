/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApiKind.REQUIRES_KNOWLEDGE
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.targets.web.KotlinWebPlatformType
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.createNpmDependenciesReportResolver
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.npmSharedDependenciesConfigurationName
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

/**
 * Creates one shared npm project per platform, JS and WasmJS independently,
 * used to resolve the npm dependencies declared in the subprojects.
 */
@ExperimentalKotlinGradlePluginApi
@DelicateKotlinGradlePluginApi(REQUIRES_KNOWLEDGE)
abstract class KotlinSharedNpmProjectPlugin
@Inject
internal constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : Plugin<Project> {

    override fun apply(project: Project) {
        // base plugin: adds `clean` task for `build/<platform>/shared-npm-project`
        project.plugins.apply(BasePlugin::class.java)

        KotlinWebPlatformType.entries.forEach { platform ->
            setupSharedNpmProject(project, platform)
        }
    }

    /**
     * Sets up the consumer side of the shared-npm-project flow on the root project for [platform].
     */
    private fun setupSharedNpmProject(
        project: Project,
        platform: KotlinWebPlatformType,
    ) {
        val npmDependenciesReportResolver = project.createNpmDependenciesReportResolver(platform)

        val rootDirectoryName = platform.platformName
        val setupSharedNpmProjectTaskName = lowerCamelCaseName("kotlin", platform.platformName, "setupSharedNpmProject")

        project.registerTask<KotlinSetupSharedNpmProjectTask>(setupSharedNpmProjectTaskName) { task ->
            task.description =
                "Assembles the '$rootDirectoryName' shared npm project from the projects declared on '${platform.npmSharedDependenciesConfigurationName}'"
            task.reportFiles.from(
                npmDependenciesReportResolver.incoming.artifactView { it.lenient(true) }.files
            )
            task.rootPackageName.convention(project.name)
            task.rootPackageVersion.convention(providers.provider { project.version.toString() })
            task.outputDirectory.convention(layout.buildDirectory.dir("$rootDirectoryName/shared-npm-project"))
            task.usesService(project.kotlinToolingDiagnosticsCollectorProvider)
            task.setupKotlinToolingDiagnosticsParameters(project)
        }
    }

}
