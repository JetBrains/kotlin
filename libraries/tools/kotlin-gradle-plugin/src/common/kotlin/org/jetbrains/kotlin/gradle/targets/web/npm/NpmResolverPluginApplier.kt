/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.hasSharedNpmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.ir.npmDependencies
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmApiExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NPM_DEP_FILE_VERSION_PREFIX
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.whenEvaluated
import org.jetbrains.kotlin.gradle.utils.withType
import java.util.*

/**
 * A utility class that applies the configuration for resolving NPM dependencies
 * within given Gradle projects. This class wires required tasks and dependencies
 * to projects using the provided resolution logic and ensures correct task execution order.
 *
 * Applied to all subprojects that have Kotlin targets that support npm dependencies.
 * (I.e. this works for both JS and WasmJS, and for both npm and Yarn package managers.)
 *
 * @property nodeJsRootApply A function that initializes the `AbstractNodeJsRootExtension`
 * for the root Node.js project configuration.
 * @property singleNodeJsApply A function to apply per-project Node.js-specific configurations.
 */
@ObsoleteNpmDependencyModel
internal class NpmResolverPluginApplier(
    private val nodeJsRootApply: (Project) -> BaseNodeJsRootExtension,
    private val singleNodeJsApply: (Project) -> Unit,
    private val requiredNpmDependenciesPredicate: (task: RequiresNpmDependenciesTask) -> Boolean,
) {
    fun apply(project: Project) {
        val nodeJsRoot = nodeJsRootApply(project)
        singleNodeJsApply(project)
        nodeJsRoot.resolver.addProject(project)
        project.whenEvaluated {
            project.tasks
                .withType<RequiresNpmDependenciesTask>()
                .matching { task ->
                    task.enabled && requiredNpmDependenciesPredicate(task)
                }
                .configureEach { task ->
                    // KotlinJsTest delegates npm dependencies to testFramework,
                    // which can be defined after this configure action
                    if (task !is KotlinJsTest) {
                        nodeJsRoot.taskRequirements.addTaskRequirements(task)

                        if (task.requiredNpmDependencies.isNotEmpty()) {
                            task.dependsOn(
                                nodeJsRoot.npmInstallTaskProvider,
                            )

                            task.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
                        }
                    }
                }
        }
    }

    /**
     * Configure the task inputs for tasks that require npm dependencies,
     * which is important for up-to-date checks and caching.
     * See [RequiresNpmDependenciesTask.npmDependenciesLockFiles]
     * and [RequiresNpmDependenciesTask.fileBasedNpmDependencyLocations].
     *
     * This method is used by both JS and WasmJS projects.
     * WasmWASI does not use npm dependencies, so WASI targets should not use this method.
     *
     * @param matcher A predicate that determines whether a [RequiresNpmDependenciesTask] should be configured.
     *
     */
    internal fun configureNpmDependencyTaskInputs(
        project: Project,
        matcher: (task: RequiresNpmDependenciesTask) -> Boolean,
    ) {
        project.tasks.withType<RequiresNpmDependenciesTask>().configureEach { task ->
            if (!matcher(task)) return@configureEach

            configureNpmDependenciesLockFiles(task)

            disableCachingIfTaskHasFileBasedNpmDependencies(task)
        }
    }

    /**
     * Must register lock files as task inputs.
     * If the npm dependencies change, the task must not be cached and must re-run.
     */
    private fun configureNpmDependenciesLockFiles(
        task: RequiresNpmDependenciesTask,
    ) {
        val packageManager: Property<NpmApiExtension<*, *>> =
            task.compilation.npmProject.nodeJsRoot.packageManagerExtension

        // add the project lock file
        task.npmDependenciesLockFiles.from(
            packageManager.map { ext ->
                ext.additionalInstallOutput
            })

        // add kgp-tooling npm dependencies lockfile, if supported by the current compilation
        if (task.compilation.hasSharedNpmToolingDir()) {
            val lockfileName =
                packageManager.flatMap { ext ->
                    ext.lockFileNameProvider
                }
            val npmToolingDir = task.compilation.npmToolingDir()
            val npmToolingLockFile =
                npmToolingDir.zip(lockfileName) { dir, name ->
                    dir.file(name)
                }
            task.npmDependenciesLockFiles.from(npmToolingLockFile)
        }
    }

    /**
     * If the task has file-based npm dependencies, disable caching (both incremental up-to-date checks, and for build cache).
     *
     * KGP does not have a good method of fine-grained tracking of file-based npm dependencies.
     * Therefore, we play it safe and disable caching for all tasks that have file-based npm dependencies.
     * Follow KT-86309 for implementing support.
     */
    private fun disableCachingIfTaskHasFileBasedNpmDependencies(
        task: RequiresNpmDependenciesTask,
    ) {
        val compilationResolver =
            task.compilation.getCompilationResolver()

        if (compilationResolver == null) {
            // If this compilation has no compilation resolver, then we can assume this task
            // is not associated with a compilation that supports npm dependencies.
            task.logger.debug("${task.path} skipping file-based npm cache check, no compilationResolver found")
            return
        }

        task.mustRunAfter(
            task.compilation.npmProject.nodeJsRoot.npmInstallTaskProvider
        )

        registerNpmFileDepsAsTaskInput(task)
        disableCachingForLegacyNpmFileDeps(task, compilationResolver)
    }

    private fun registerNpmFileDepsAsTaskInput(task: RequiresNpmDependenciesTask) {
        val npmDepsFromCollector: Provider<List<Provider<Directory>>> =
            task.compilation.npmDependencies()
                .map {
                    it.filterIsInstance<KotlinNpmDependency.LocalPath>()
                        .map(KotlinNpmDependency.LocalPath::directory)
                }

        task.fileBasedDependencyDirectories.from(npmDepsFromCollector)
    }

    private fun disableCachingForLegacyNpmFileDeps(task: RequiresNpmDependenciesTask, compilationResolver: KotlinCompilationNpmResolver) {
        //region legacy configuration-based npm deps
        val npmDepsFromConfigurations =
            compilationResolver
                .aggregatedConfiguration
                .allDependencies
                .withType<@Suppress("DEPRECATION") org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency>()

        // Use `providers.provider {}`, not `npmDepsFromConfigurations.elements`:
        // the configuration cache cannot encode the `elements` provider of a Configuration's dependencies.
        val fileBasedDependenciesFromConfigurations: Provider<List<String>> =
            task.project.providers.provider {
                npmDepsFromConfigurations
                    .map { it.version }
                    .filter { it.startsWith(NPM_DEP_FILE_VERSION_PREFIX) }
                    .map { it.removePrefix(NPM_DEP_FILE_VERSION_PREFIX) }
                    .distinct()
            }

        task.fileBasedNpmDependencyLocations.convention(
            fileBasedDependenciesFromConfigurations
        )

        task.inputs.property(
            "fileBasedNpmDependencyLocationsRerunToken",
            task.project.providers.of(FileBasedNpmDependencyLocationsRerunTokenSource::class.java) { spec ->
                spec.parameters.fileBasedNpmDependencyLocations.set(task.fileBasedNpmDependencyLocations)
            })

        task.outputs.doNotCacheIf("there are file-based NPM dependencies") { _ ->
            val fileBasedNpmDependencyLocations = task.fileBasedNpmDependencyLocations.getOrElse(emptyList())
            val disableCache = fileBasedNpmDependencyLocations.isNotEmpty()
            if (disableCache) {
                task.logger.info(
                    "${task.path} doNotCacheIf=true. " +
                            "Task depends on file-based NPM dependencies: $fileBasedNpmDependencyLocations. " +
                            "See KT-86309."
                )
            }
            disableCache
        }
    }

    /**
     * Changes on every call if there are any [RequiresNpmDependenciesTask.fileBasedNpmDependencyLocations], so that the task always re-runs.
     *
     * Avoid using [org.gradle.api.tasks.TaskOutputs.upToDateWhen] because if
     * [RequiresNpmDependenciesTask.fileBasedNpmDependencyLocations] is empty then the task will be marked as up-to-date,
     * even if the task has no outputs and should always rerun.
     *
     * This is a temporary workaround that should be removed when [RequiresNpmDependenciesTask] is removed.
     */
    internal abstract class FileBasedNpmDependencyLocationsRerunTokenSource :
        ValueSource<String, FileBasedNpmDependencyLocationsRerunTokenSource.Parameters> {

        interface Parameters : ValueSourceParameters {
            val fileBasedNpmDependencyLocations: ListProperty<String>
        }

        override fun obtain(): String =
            if (parameters.fileBasedNpmDependencyLocations.getOrElse(emptyList()).isEmpty()) {
                ""
            } else {
                // just return something random with high entropy, so the task is never up-to-date
                UUID.randomUUID().toString()
            }
    }

    @ObsoleteNpmDependencyModel
    companion object {
        private fun KotlinJsIrCompilation.getCompilationResolver(): KotlinCompilationNpmResolver? =
            npmProject.nodeJsRoot.resolver.getOrNull(project.path)?.getOrNull(disambiguatedName)
    }
}
