/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.isolated

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.web.npm.internal.NpmProjectLayout
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.tasks.withType
import java.io.File

/**
 * A task that resolves npm dependencies through [KotlinAggregatedNpmWorkspaceService].
 *
 * Every project of the build may use the service, it is enough to implement this interface:
 * the task is wired to the service and to the npm installation by [KotlinAggregatedNpmWorkspaceService.useFromTasks].
 */
internal interface UsesKotlinAggregatedNpmWorkspaceService : Task {
    @get:Internal
    val aggregatedNpmWorkspaceService: Property<KotlinAggregatedNpmWorkspaceService>
}

/**
 * Provides access to the npm workspaces of the shared npm root project, in which the npm dependencies
 * of every compilation of the build are installed.
 *
 * The service is intentionally parameter-less, so that any project of the build can register it
 * without knowing anything about the project that owns the shared npm root project.
 * It is [KotlinIsolatedNpmInstallTask] that performs the installation and tells the service
 * where the npm dependencies are installed, see [npmInstalled].
 *
 * Besides resolving node modules, the service also owns the compiled JS files of a compilation:
 * [syncCompiledJsFiles] puts them into the npm workspace of that compilation, so that the JS runtime
 * executes them in the context of the workspace, where all the npm dependencies are installed and configured.
 *
 * Unlike the legacy NPM resolution, a consumer does not need to know where the npm dependencies are installed,
 * and therefore does not need to access the project that owns the shared npm root project,
 * which makes the service compatible with Gradle Isolated Projects.
 */
internal abstract class KotlinAggregatedNpmWorkspaceService : BuildService<BuildServiceParameters.None> {

    @Volatile
    private var sharedNpmProjectDirectory: File? = null

    /**
     * Called by [KotlinIsolatedNpmInstallTask] once the npm dependencies of the shared npm root project
     * located in [directory] are installed.
     */
    fun npmInstalled(directory: File) {
        sharedNpmProjectDirectory = directory
    }

    private val installedSharedNpmProjectDirectory: File
        get() = sharedNpmProjectDirectory ?: error(
            "The npm dependencies are not installed yet. " +
                    "A task that uses ${KotlinAggregatedNpmWorkspaceService::class.java.simpleName} " +
                    "must depend on '${KotlinIsolatedNpmInstallTask.TASK_PATH}'."
        )

    /**
     * The npm workspace of the compilation whose npm package is named [npmProjectName].
     *
     * Modules are resolved from the workspace directory, so that the resolution walks up
     * to the hoisted `node_modules` of the shared npm root project,
     * exactly like it does in the legacy NPM resolution.
     */
    fun npmProjectModules(npmProjectName: String): NpmProjectModules =
        NpmProjectModules(npmProjectDirectory(npmProjectName))

    /**
     * The directory of the npm workspace of the compilation whose npm package is named [npmProjectName].
     */
    fun npmProjectDirectory(npmProjectName: String): File =
        installedSharedNpmProjectDirectory.resolve(NpmProjectLayout.PACKAGES).resolve(npmProjectName)

    /**
     * The directory of the npm workspace of the compilation whose npm package is named [npmProjectName]
     * that contains the compiled JS files put there by [syncCompiledJsFiles].
     */
    fun npmProjectDistDirectory(npmProjectName: String): File =
        npmProjectDirectory(npmProjectName).resolve(NpmProject.DIST_FOLDER)

    /**
     * Copies the compiled JS files of the compilation whose npm package is named [npmProjectName]
     * from [sourceDirectories] into its npm workspace.
     *
     * This is the Isolated Projects compatible counterpart of the legacy sync of
     * [org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink] outputs into the npm root project:
     * the JS runtime can only resolve the installed npm dependencies of a compilation
     * when it executes its files from within the npm workspace of that compilation.
     *
     * Stale files are intentionally not removed, because several binaries of the same compilation
     * share one npm workspace, exactly like they share one directory in the legacy NPM resolution.
     */
    fun syncCompiledJsFiles(npmProjectName: String, sourceDirectories: Iterable<File>) {
        val distDirectory = npmProjectDistDirectory(npmProjectName)
        distDirectory.mkdirs()

        sourceDirectories
            .filter { it.isDirectory }
            .forEach { sourceDirectory ->
                sourceDirectory.copyRecursively(distDirectory, overwrite = true)
            }
    }

    /**
     * Requires the [request] node module and returns the canonical path to its main js file.
     */
    fun require(request: String): String =
        NpmProjectModules(installedSharedNpmProjectDirectory).require(request)

    companion object {
        private val serviceClass = KotlinAggregatedNpmWorkspaceService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

        /**
         * Registers the service, which may be done by any project of the build.
         */
        fun registerIfAbsent(project: Project): Provider<KotlinAggregatedNpmWorkspaceService> =
            project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {}

        /**
         * Makes every [UsesKotlinAggregatedNpmWorkspaceService] task of [project] use the service,
         * and depend on the installation of the npm dependencies.
         */
        fun useFromTasks(project: Project) {
            val serviceProvider = registerIfAbsent(project)
            SingleActionPerProject.run(project, UsesKotlinAggregatedNpmWorkspaceService::class.java.name) {
                project.tasks.withType<UsesKotlinAggregatedNpmWorkspaceService>().configureEach { task ->
                    task.aggregatedNpmWorkspaceService.value(serviceProvider).disallowChanges()
                    task.usesService(serviceProvider)

                    // The installation task itself configures the service, so it must not depend on itself.
                    if (task !is KotlinIsolatedNpmInstallTask) {
                        // The npm dependencies of the whole build are installed by a single task
                        // of the project that applies `KotlinSharedNpmProjectPlugin`.
                        // It is referenced by path, because that project cannot be accessed
                        // when Gradle Isolated Projects are enabled.
                        task.dependsOn(KotlinIsolatedNpmInstallTask.TASK_PATH)
                    }
                }
            }
        }
    }
}
