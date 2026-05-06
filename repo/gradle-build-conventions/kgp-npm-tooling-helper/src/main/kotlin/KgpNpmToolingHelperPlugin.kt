/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.kgpnpmtooling

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.task.NodeSetupTask
import com.github.gradle.node.yarn.task.YarnSetupTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.build.kgpnpmtooling.tasks.GenerateNpmVersionsKotlinClassTask
import org.jetbrains.kotlin.build.kgpnpmtooling.tasks.PrepareNpmToolingLockFilesTask
import org.jetbrains.kotlin.build.kgpnpmtooling.tasks.UpdateNpmToolingDependenciesTask
import java.io.File
import javax.inject.Inject

abstract class KgpNpmToolingHelperPlugin
@Inject
internal constructor(
    private val objects: ObjectFactory,
    private val layout: ProjectLayout,
) : Plugin<Project> {

    override fun apply(project: Project) {
        setupGradleNodePlugin(project)

        val extension = createExtension(project)

        val nodeExecutable = getNodeExecutable(project)

        registerKgpNpmToolingTasks(project)
        configureTaskConventions(
            project = project,
            extension = extension,
            nodeExecutable = nodeExecutable,
        )
    }

    private fun createExtension(project: Project): NpmVersionsCodegenExtension {
        return project.extensions.create("kgpNpmTooling", NpmVersionsCodegenExtension::class).apply {
            npmToolingProjectDir.convention(
                layout.projectDirectory.dir("kotlin-npm-tooling")
            )
        }
    }

    /**
     * Get the `node`/`node.exe` executable for the current OS from the `com.github.node-gradle.node` plugin.
     */
    private fun getNodeExecutable(project: Project): Provider<RegularFile> {

        val nodeSetupTask = project.tasks.named<NodeSetupTask>(NodeSetupTask.NAME)

        val isCurrentOsWindows = System.getProperty("os.name").lowercase().contains("windows")
        val pathToNode = if (isCurrentOsWindows) "bin/node.exe" else "bin/node"

        return nodeSetupTask.flatMap {
            it.nodeDir.file(pathToNode)
        }
    }

    private fun setupGradleNodePlugin(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        project.extensions.configure<NodeExtension> {
            download.set(true)
        }
    }

    private fun registerKgpNpmToolingTasks(project: Project) {
        project.tasks.register("updateKgpNpmToolingDependencies", UpdateNpmToolingDependenciesTask::class)
        project.tasks.register("prepareKgpNpmToolingLockFiles", PrepareNpmToolingLockFilesTask::class)
        project.tasks.register("generateNpmVersionsKotlinClass", GenerateNpmVersionsKotlinClassTask::class)
    }

    private fun configureTaskConventions(
        project: Project,
        extension: NpmVersionsCodegenExtension,
        nodeExecutable: Provider<RegularFile>,
    ) {
        project.tasks.withType<GenerateNpmVersionsKotlinClassTask>().configureEach { t ->
            t.group = TASK_GROUP
            t.copyrightHeader.convention(layout.settingsDirectory.file("license/COPYRIGHT_HEADER.txt"))
            t.outputDir.convention(t.temporaryDir)
            t.npmToolingProjectDir.convention(extension.npmToolingProjectDir)
        }

        project.tasks.withType<PrepareNpmToolingLockFilesTask>().configureEach { t ->
            t.group = TASK_GROUP
            t.description = "Prepares NPM tooling lock files for Kotlin Gradle Plugin"
            t.relativePathToBaseLockFilesDir.convention("org/jetbrains/kotlin/gradle/targets/js/")
            t.npmToolingProjectDir.convention(extension.npmToolingProjectDir)
            t.outputDir.convention(t.temporaryDir)
        }

        val nodeSetupTask = project.tasks.named<NodeSetupTask>(NodeSetupTask.NAME)
        val yarnSetupTask = project.tasks.named<YarnSetupTask>(YarnSetupTask.NAME)
        project.tasks.withType<UpdateNpmToolingDependenciesTask>().configureEach { t ->
            t.group = TASK_GROUP
            t.description = "Upgrades Kotlin NPM tooling dependencies to the latest versions. This task must be run manually."
            t.nodeExecutable.convention(nodeExecutable)
            t.npmCli.convention(
                nodeSetupTask.flatMap { it.nodeDir.file("bin/npm") }
            )
            t.yarnCli.convention(
                // Must use `.map {}`, not `.flatMap {}` https://github.com/gradle/gradle/issues/37787
                yarnSetupTask.map { it.yarnDir.get().file("bin/yarn") }
            )
            t.npmToolingProjectDir.convention(extension.npmToolingProjectDir)
            t.updateVersions.convention(false)
        }
    }

    private fun DirectoryProperty.convention(file: File): DirectoryProperty =
        convention(objects.directoryProperty().fileValue(file))

    companion object {
        private const val TASK_GROUP = "kgp-npm-tooling"
    }
}
