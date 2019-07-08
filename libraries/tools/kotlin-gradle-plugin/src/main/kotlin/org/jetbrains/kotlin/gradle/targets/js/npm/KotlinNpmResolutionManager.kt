/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinProjectNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import java.io.File

/**
 * # NPM resolution state manager
 *
 * ## Resolving process from Gradle
 *
 * **configuring**. Global initial state. [NpmResolverPlugin] should be applied for each project
 * that requires NPM resolution. When applied, [KotlinProjectNpmResolver] will be created for the
 * corresponding project and will subscribe to all js compilations. [NpmResolverPlugin] requires
 * kotlin mulitplatform or plaform plugin applied first.
 *
 * **up-to-date-checked**. This state is compilation local: one compilation may be in up-to-date-checked
 * state, while another may be steel in configuring state. New compilations may be added in this
 * state, but compilations that are already up-to-date-checked cannot be changed.
 * Initiated by calling [KotlinPackageJsonTask.producerInputs] getter (will be called by Gradle).
 * [KotlinCompilationNpmResolver] will create and **resolve** aggregated compilation configuration,
 * which contains all related compilation configuration and NPM tools configuration.
 * NPM tools configuration contains all dependencies that is required for all enabled
 * tasks related to this compilation. It is important to resolve this configuration inside particular
 * project and not globally. Aggregated configuration will be analyzed for gradle internal dependencies
 * (project dependencies), gradle external dependencies and npm dependencies. This collections will
 * be treated as `packageJson` task inputs.
 *
 * **package-json-created**. This state also compilation local. Initiated by executing `packageJson`
 * task for particular compilation. If `packageJson` task is up-to-date, this state is reached by
 * first calling [KotlinCompilationNpmResolver.getResolutionOrResolveIfForced] which may be called
 * by compilation that depends on this compilation. Note that package.json will be executed only for
 * required compilations, while other may be missed.
 *
 * **installed**. Global final state. Initiated by executing global `kotlinNpmInstall` task.
 * All created package.json files will be gathered and package manager will be executed.
 * Package manager will create lock file, that will be parsed for transitive npm dependencies
 * that will be added to the root [NpmDependency] objects. `kotlinNpmInstall` task may be up-to-date.
 * In this case, installed state will be reached by first call of [installIfNeeded] without executing
 * package manager.
 *
 * Resolution will be used from [NpmDependency] by calling [getNpmDependencyResolvedCompilation].
 * Also resolution will be checked before calling [NpmProject.require] and executing any task
 * that requires npm dependencies or node_modules.
 *
 * User can call [requireInstalled] to get resolution info.
 *
 * ## Resolving process during Idea import
 *
 * In this case [forceFullResolve] will be set, and all compilations will be resolved
 * even without `packageJson` task execution.
 *
 * During building gradle project model, all [NpmDependency] will be asked for there files,
 * and [getNpmDependencyResolvedCompilation] will be called. In the [forceFullResolve] mode
 * project will be fully resolved: all `package.json` files will be created, and package manager
 * will be called. We are manually tracking package.json files contents to avoid calling package manager
 * if nothing was changes.
 *
 * Note that in this case resolution process will be performed outside of task execution.
 */
class KotlinNpmResolutionManager(private val nodeJsSettings: NodeJsRootExtension) {
    private val forceFullResolve: Boolean
        get() = isInIdeaSync

    @Volatile
    private var state: ResolutionState =
        ResolutionState.Configuring(
            KotlinRootNpmResolver(nodeJsSettings, forceFullResolve)
        )

    internal sealed class ResolutionState {
        class Configuring(val resolver: KotlinRootNpmResolver) : ResolutionState()
        class Installed(val resolved: KotlinRootNpmResolution) : ResolutionState()
    }

    @Incubating
    fun requireInstalled() = installIfNeeded(requireUpToDateReason = "")

    internal fun requireConfiguringState(): KotlinRootNpmResolver =
        (this.state as? ResolutionState.Configuring ?: error("NPM Dependencies already resolved and installed")).resolver

    internal fun install() = installIfNeeded(requireNotInstalled = true)

    internal fun requireAlreadyInstalled(project: Project, reason: String = ""): KotlinProjectNpmResolution =
        installIfNeeded(requireUpToDateReason = reason)[project]

    internal val packageJsonFiles: Collection<File>
        get() = (state as ResolutionState.Configuring).resolver.compilations.map { it.npmProject.packageJsonFile }

    /**
     * @param requireUpToDateReason Check that project already resolved,
     * or it is up-to-date but just not closed. Show given message if it is not.
     * @param requireNotInstalled Check that project is not resolved
     */
    private fun installIfNeeded(
        requireUpToDateReason: String? = null,
        requireNotInstalled: Boolean = false
    ): KotlinRootNpmResolution {
        fun alreadyResolved(resolution: KotlinRootNpmResolution): KotlinRootNpmResolution {
            if (requireNotInstalled) error("Project already resolved")
            return resolution
        }

        val state0 = this.state
        return when (state0) {
            is ResolutionState.Installed -> alreadyResolved(state0.resolved)
            is ResolutionState.Configuring -> {
                synchronized(this) {
                    val state1 = this.state
                    when (state1) {
                        is ResolutionState.Installed -> alreadyResolved(state1.resolved)
                        is ResolutionState.Configuring -> {
                            val upToDate = nodeJsSettings.npmInstallTask.state.upToDate
                            if (requireUpToDateReason != null && !upToDate) {
                                error("NPM dependencies should be resolved $requireUpToDateReason")
                            }

                            val forceUpToDate = upToDate && !forceFullResolve
                            state1.resolver.close(forceUpToDate).also {
                                this.state = ResolutionState.Installed(it)
                                state1.resolver.closePlugins(it)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun getNpmDependencyResolvedCompilation(npmDependency: NpmDependency): KotlinCompilationNpmResolution? {
        val project = npmDependency.project

        val resolvedProject =
            if (forceFullResolve) {
                installIfNeeded()[project]
            } else {
                // may return null only during npm resolution
                // (it can be called since NpmDependency added to configuration that
                // requires resolve to build package.json, in this case we should just skip this call)
                val state0 = state
                when (state0) {
                    is ResolutionState.Installed -> state0.resolved[project]
                    is ResolutionState.Configuring -> {
                        return null
                        //error("Cannot use NpmDependency before :kotlinNpmInstall task execution")
                    }
                }
            }

        return resolvedProject.npmProjectsByNpmDependency[npmDependency] ?: error("NPM project resolved without $this")
    }

    internal fun <T> checkRequiredDependencies(task: T)
            where T : RequiresNpmDependencies,
                  T : Task {
        val project = task.project
        val requestedTaskDependencies = requireAlreadyInstalled(project, "before $task execution").taskRequirements
        val targetRequired = requestedTaskDependencies[task]?.toSet() ?: setOf()

        task.requiredNpmDependencies.forEach {
            check(it in targetRequired) {
                "${it.createDependency(project)} required by $task was not found resolved at the time of nodejs package manager call. " +
                        "This may be caused by changing $task configuration after npm dependencies resolution."
            }
        }
    }
}