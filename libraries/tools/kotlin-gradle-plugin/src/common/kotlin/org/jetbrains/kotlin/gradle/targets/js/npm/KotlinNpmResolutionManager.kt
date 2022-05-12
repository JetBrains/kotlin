/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Incubating
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.Logger
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinProjectNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.utils.unavailableValueError
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
 * **Prepared**.
 * Global final state. Initiated by executing global `rootPackageJson` task.
 *
 * **Installed**.
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
class KotlinNpmResolutionManager(@Transient private val nodeJsSettings: NodeJsRootExtension?) {
    private val forceFullResolve: Boolean by lazy {
        (nodeJsSettings ?: unavailableValueError("nodeJsSettings")).rootProject.isInIdeaSync
    }

    internal val resolver = KotlinRootNpmResolver(nodeJsSettings, forceFullResolve)

    internal abstract class KotlinNpmResolutionManagerStateHolder : BuildService<BuildServiceParameters.None> {
        @Volatile
        internal var state: ResolutionState? = null
    }

    private val stateHolderProvider = (nodeJsSettings ?: unavailableValueError("nodeJsSettings"))
        .rootProject.gradle.sharedServices.registerIfAbsent(
            "npm-resolution-manager-state-holder", KotlinNpmResolutionManagerStateHolder::class.java
        ) {
        }

    private val stateHolder get() = stateHolderProvider.get()

    private var state: ResolutionState
        get() = stateHolder.state ?: ResolutionState.Configuring(resolver)
        set(value) {
            stateHolder.state = value
        }

    internal sealed class ResolutionState {
        abstract val npmProjects: List<NpmProject>

        class Configuring(val resolver: KotlinRootNpmResolver) : ResolutionState() {
            override val npmProjects: List<NpmProject>
                get() = resolver.compilations.map { it.npmProject }
        }

        open class Prepared(val preparedInstallation: KotlinRootNpmResolver.Installation) : ResolutionState() {
            override val npmProjects: List<NpmProject>
                get() = npmProjectsByProjectResolutions(preparedInstallation.projectResolutions)
        }

        class Installed(val resolved: KotlinRootNpmResolution) : ResolutionState() {
            override val npmProjects: List<NpmProject>
                get() = npmProjectsByProjectResolutions(resolved.projects)
        }

        companion object {
            fun npmProjectsByProjectResolutions(
                resolutions: Map<String, KotlinProjectNpmResolution>
            ): List<NpmProject> {
                return resolutions
                    .values
                    .flatMap { it.npmProjects.map { it.npmProject } }
            }
        }
    }

    @Incubating
    internal fun requireInstalled(
        services: ServiceRegistry,
        logger: Logger,
        reason: String = ""
    ) = installIfNeeded(reason = reason, services = services, logger = logger)

    internal fun requireConfiguringState(): KotlinRootNpmResolver =
        (this.state as? ResolutionState.Configuring ?: error("NPM Dependencies already resolved and installed")).resolver

    internal fun isConfiguringState(): Boolean =
        this.state is ResolutionState.Configuring

    internal fun prepare(logger: Logger) = prepareIfNeeded(requireNotPrepared = true, logger = logger)

    internal fun installIfNeeded(
        reason: String? = "",
        args: List<String> = emptyList(),
        services: ServiceRegistry,
        logger: Logger
    ): KotlinRootNpmResolution {
        synchronized(stateHolder) {
            if (state is ResolutionState.Installed) {
                return (state as ResolutionState.Installed).resolved
            }

            val installUpToDate = nodeJsSettings?.npmInstallTaskProvider?.get()?.state?.upToDate ?: false
            val forceUpToDate = installUpToDate && !forceFullResolve

            val installation = prepareIfNeeded(requireUpToDateReason = reason, logger = logger)
            val resolution = installation
                .install(forceUpToDate, args, services, logger)
            state = ResolutionState.Installed(resolution)

            installation.closePlugins(resolution)

            return resolution
        }
    }

    internal val packageJsonFiles: Collection<File>
        get() = state.npmProjects.map { it.packageJsonFile }

    /**
     * @param requireUpToDateReason Check that project already resolved,
     * or it is up-to-date but just not closed. Show given message if it is not.
     * @param requireNotPrepared Check that project is not prepared
     */
    private fun prepareIfNeeded(
        requireUpToDateReason: String? = null,
        requireNotPrepared: Boolean = false,
        logger: Logger
    ): KotlinRootNpmResolver.Installation {
        fun alreadyResolved(installation: KotlinRootNpmResolver.Installation): KotlinRootNpmResolver.Installation {
            if (requireNotPrepared) error("Project already prepared")
            return installation
        }

        val state0 = this.state
        return when (state0) {
            is ResolutionState.Prepared -> {
                alreadyResolved(state0.preparedInstallation)
            }
            is ResolutionState.Configuring -> {
                synchronized(stateHolder) {
                    val state1 = this.state
                    when (state1) {
                        is ResolutionState.Prepared -> alreadyResolved(state1.preparedInstallation)
                        is ResolutionState.Configuring -> {
                            val upToDate = nodeJsSettings?.rootPackageJsonTaskProvider?.get()?.state?.upToDate ?: true
                            if (requireUpToDateReason != null && !upToDate) {
                                error("NPM dependencies should be resolved $requireUpToDateReason")
                            }

                            state1.resolver.prepareInstallation(logger).also {
                                this.state = ResolutionState.Prepared(it)
                            }
                        }
                        is ResolutionState.Installed -> error("Project already installed")
                    }
                }
            }
            is ResolutionState.Installed -> error("Project already installed")
        }
    }

    internal fun getNpmDependencyResolvedCompilation(npmDependency: NpmDependency): KotlinCompilationNpmResolution? {
        val project = npmDependency.project!!
        val projectPath = project.path
        val services = (project as ProjectInternal).services
        val logger = project.logger

        val resolvedProject =
            if (forceFullResolve) {
                installIfNeeded(reason = null, services = services, logger = logger)[projectPath]
            } else {
                // may return null only during npm resolution
                // (it can be called since NpmDependency added to configuration that
                // requires resolve to build package.json, in this case we should just skip this call)
                val state0 = state
                when (state0) {
                    is ResolutionState.Prepared -> state0.preparedInstallation[projectPath]
                    is ResolutionState.Configuring -> {
                        return null
                        //error("Cannot use NpmDependency before :kotlinNpmInstall task execution")
                    }
                    is ResolutionState.Installed -> state0.resolved[projectPath]
                }
            }

        return resolvedProject.npmProjectsByNpmDependency[npmDependency] ?: error("NPM project resolved without $this")
    }

    internal fun <T> checkRequiredDependencies(task: T, services: ServiceRegistry, logger: Logger, projectPath: String)
            where T : RequiresNpmDependencies,
                  T : Task {
        val targetRequired = resolver.taskRequirements.byTask[task.path]?.toSet() ?: setOf()

        task.requiredNpmDependencies.forEach {
            check(it in targetRequired) {
                "${it.createDependency(task.project)} required by $task was not found resolved at the time of nodejs package manager call. " +
                        "This may be caused by changing $task configuration after npm dependencies resolution."
            }
        }
    }
}