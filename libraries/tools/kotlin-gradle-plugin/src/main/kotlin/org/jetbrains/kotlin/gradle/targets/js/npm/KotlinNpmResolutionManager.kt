/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

/**
 * # NPM resolution state manager
 *
 * ## Resolving process from Gradle
 *
 * **configuring**. Initial state. [NpmResolverPlugin] should be applied for each project
 * that requires NPM resolution. This plugin should be applied only after kotlin plugin.
 * When applied, [KotlinProjectNpmResolver] will be created for the corresponding project
 * and will subscribe to all js compilations.
 *
 * **up-to-date-checked**. This state is compilation local (one compilation may be in up-to-date-checked
 * state, while another may be steel in configuring state). New compilations may be added in this
 * state, but compilations that are already up-to-date-checked cannot be changes.
 * Initiated by calling [KotlinPackageJsonTask.producerInputs] getter (will be called by Gradle).
 * [KotlinCompilationNpmResolver] will create and **resolve** aggregated compilation configuration,
 * which consists of all related compilation configuration
 * and npm tools configuration which contains all dependencies that is required for all enabled
 * tasks related to this compilation. It is important to resolve this configuration inside particular
 * project and not globally. Aggregated configuration will be analyzed for gradle internal dependencies
 * (project depenencies), gradle external dependencies and npm dependencies. This collections will
 * be treated as `packageJson` task inputs.
 *
 * **package-json-created**. This state also compilation local. Initiated by executing `packageJson`
 * task for particular compilation.
 *
 * Note that package.json will be executed only for required compilations, while other may be missed.
 *
 * **installing**
 *
 * **installed**
 *
 *
 *
 * - resolving:
 *   - [KotlinPackageJsonTask] task executed for each affected compilation:
 *      - ensure that all needed package.json files created
 *   - global [KotlinNpmInstallTask] executed:
 *      - call package manager to sync node_modules
 *
 * Resolved state requested by calling [requireAlreadyInstalled] from this places:
 *  - tasks that requires node_modules
 *  - before calling [NpmProject.require]
 *
 * Also resolved state used when getting files of [NpmDependency]. In this case
 * resolved project is not required. [NpmDependency] will not return any files if
 * ... [requireAlreadyResolvedOrNullIfResolvingNow]
 *
 * Note that [KotlinPackageJsonTask] executed for required compilations only.
 * [KotlinNpmInstallTask] may not be executed if none of package.json tasks executed.
 * In this case [_resolutionState] will remain unclosed and will be closed
 * on first [requireAlreadyInstalled] call with check that everything is already up-to-date.
 *
 *
 *
 * ## Resolving process during Idea import
 */
class KotlinNpmResolutionManager(val nodeJsSettings: NodeJsRootExtension) {
    internal val resolutionState: ResolutionState
        get() = _resolutionState

    private val forceFullResolve: Boolean
        get() = isInIdeaSync

    @Volatile
    private var _resolutionState: ResolutionState =
        ResolutionState.Configuring(
            KotlinRootNpmResolver(nodeJsSettings, forceFullResolve)
        )

    internal sealed class ResolutionState : ResolutionStateData {
        class Configuring(val resolver: KotlinRootNpmResolver) : ResolutionState(), ResolutionStateData by resolver
        class Installing(val resolver: KotlinRootNpmResolver) : ResolutionState(), ResolutionStateData by resolver
        class Installed(val resolved: KotlinRootNpmResolution) : ResolutionState(), ResolutionStateData by resolved
    }

    interface ResolutionStateData {
        val compilations: Collection<KotlinJsCompilation>
    }

    internal fun requireConfiguringState(): KotlinRootNpmResolver =
        (resolutionState as? ResolutionState.Configuring ?: error("NPM Dependencies already resolved and installed")).resolver

    /**
     * @param requireUpToDateReason Check that project already resolved,
     * or it is up-to-date but just not closed. Show given message if it is not.
     * @param requireNotInstalled Check that project is not resolved
     */
    internal fun installIfNeeded(
        requireUpToDateReason: String? = null,
        requireNotInstalled: Boolean = false
    ): KotlinRootNpmResolution {
        fun alreadyResolved(resolution: KotlinRootNpmResolution): KotlinRootNpmResolution {
            if (requireNotInstalled) error("Project already resolved")
            return resolution
        }

        val state0 = _resolutionState
        val resolution = when (state0) {
            is ResolutionState.Installed -> alreadyResolved(state0.resolved)
            is ResolutionState.Installing,
            is ResolutionState.Configuring -> {
                synchronized(this) {
                    val state1 = _resolutionState
                    when (state1) {
                        is ResolutionState.Installed -> alreadyResolved(state1.resolved)
                        is ResolutionState.Installing -> error("Resolution state sync failed")
                        is ResolutionState.Configuring -> {
                            val state2 =
                                ResolutionState.Installing(state1.resolver)
                            _resolutionState = state2
                            state1.resolver.close().also {
                                check(_resolutionState == state2) {
                                    "Resolution state sync failed: $_resolutionState != $state2"
                                }
                                _resolutionState =
                                    ResolutionState.Installed(it)
                                if (requireUpToDateReason != null && !it.wasUpToDate) {
                                    error("NPM dependencies should be resolved $requireUpToDateReason")
                                }
                            }
                        }
                    }
                }
            }
        }

        return resolution
    }

    internal fun requireAlreadyInstalled(project: Project, reason: String = ""): KotlinProjectNpmResolution =
        installIfNeeded(requireUpToDateReason = reason)[project]

    internal fun getNpmDependencyResolvedCompilation(npmDependency: NpmDependency): KotlinCompilationNpmResolution {
        val project = npmDependency.project

        val resolvedProject =
            if (forceFullResolve) {
                installIfNeeded()[project]
            } else {
                // may return null only during npm resolution
                // (it can be called since NpmDependency added to configuration that
                // requires resolve to build package.json, in this case we should just skip this call)
                val state0 = resolutionState
                when (state0) {
                    is ResolutionState.Installed -> state0.resolved[project]
                    is ResolutionState.Installing -> null
                    is ResolutionState.Configuring -> error("Cannot use NpmDependency before :kotlinNpmInstall task execution")
                }
            }

        return when (resolvedProject) {
            null -> null
            else -> resolvedProject.npmProjectsByNpmDependency[npmDependency] ?: error("NPM project resolved without $this")
        }
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