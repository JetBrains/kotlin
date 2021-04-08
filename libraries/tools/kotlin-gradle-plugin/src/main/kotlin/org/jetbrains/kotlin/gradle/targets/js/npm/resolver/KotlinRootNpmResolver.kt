/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dukat.DukatRootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.CompositeNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.toVersionString
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.gradle.utils.FileSystemOperationsCompat

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
internal class KotlinRootNpmResolver internal constructor(
    val nodeJs: NodeJsRootExtension,
    val forceFullResolve: Boolean
) {
    val rootProject: Project
        get() = nodeJs.rootProject

    val rootProjectName by lazy {
        rootProject.name
    }

    val rootProjectVersion by lazy {
        rootProject.version.toString()
    }

    enum class State {
        CONFIGURING,
        PROJECTS_CLOSED,
        INSTALLED
    }

    @Transient
    @Volatile
    private var state_: State? = State.CONFIGURING

    private var state
        get() = state_ ?: resolverStateHolder.get().state
        set(value) {
            if (state_ != null) {
                state_ = value
            } else {
                resolverStateHolder.get().state = value
            }
        }

    private val archiveOperations by lazy { ArchiveOperationsCompat(rootProject) }
    private val fs by lazy { FileSystemOperationsCompat(rootProject) }

    internal val gradleNodeModulesProvider: Provider<GradleNodeModulesCache> =
        rootProject.gradle.sharedServices.registerIfAbsent("gradle-node-modules", GradleNodeModulesCache::class.java) {
            it.parameters.cacheDir.set(nodeJs.nodeModulesGradleCacheDir)
            it.parameters.rootProjectDir.set(rootProject.projectDir)
        }

    val gradleNodeModules: GradleNodeModulesCache
        get() = gradleNodeModulesProvider.get().also {
            it.archiveOperations = archiveOperations
            it.fs = fs
        }

    internal val compositeNodeModulesProvider: Provider<CompositeNodeModulesCache> =
        rootProject.gradle.sharedServices.registerIfAbsent("composite-node-modules", CompositeNodeModulesCache::class.java) {
            it.parameters.cacheDir.set(nodeJs.nodeModulesGradleCacheDir)
            it.parameters.rootProjectDir.set(rootProject.projectDir)
        }

    val compositeNodeModules: CompositeNodeModulesCache
        get() = compositeNodeModulesProvider.get()

    @Suppress("RedundantNullableReturnType")
    @Transient
    private val plugins_: MutableList<RootResolverPlugin>? = mutableListOf<RootResolverPlugin>().also {
        it.add(DukatRootResolverPlugin(forceFullResolve))
    }

    @Suppress("RedundantNullableReturnType")
    @Transient
    private val projectResolvers_: MutableMap<String, KotlinProjectNpmResolver>? = mutableMapOf()

    private val resolverStateHolder by lazy {
        rootProject.gradle.sharedServices.registerIfAbsent(
            KotlinRootNpmResolverStateHolder::class.qualifiedName,
            KotlinRootNpmResolverStateHolder::class.java
        ) {
            it.parameters.plugins.set(plugins_)
            it.parameters.projectResolvers.set(projectResolvers_)
        }
    }

    private val configurationCacheProjectResolvers: MutableMap<String, KotlinProjectNpmResolver>
        get() {
            val stateHolder = resolverStateHolder.get()
            val projResolvers = stateHolder.parameters.projectResolvers.get()
            if (stateHolder.initialized) return projResolvers
            projResolvers.forEach { (_, value) ->
                value.resolver = this
                value.compilationResolvers.forEach { compResolver ->
                    compResolver.rootResolver = this
                }
            }
            stateHolder.initialized = true
            return projResolvers
        }

    val plugins
        get() = plugins_ ?: resolverStateHolder.get().parameters.plugins.get()

    val projectResolvers
        get() = projectResolvers_ ?: configurationCacheProjectResolvers

    val yarn by lazy {
        YarnPlugin.apply(rootProject)
    }

    fun alreadyResolvedMessage(action: String) = "Cannot $action. NodeJS projects already resolved."

    @Synchronized
    fun addProject(target: Project) {
        check(state == State.CONFIGURING) { alreadyResolvedMessage("add new project: $target") }
        projectResolvers[target.path] = KotlinProjectNpmResolver(target, this)
    }

    operator fun get(projectPath: String) = projectResolvers[projectPath] ?: error("$projectPath is not configured for JS usage")

    val compilations: Collection<KotlinJsCompilation>
        get() = projectResolvers.values.flatMap { it.compilationResolvers.map { it.compilation } }

    fun findDependentResolver(src: Project, target: Project): List<KotlinCompilationNpmResolver>? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val targetResolver = this[target.path]
        val mainCompilations = targetResolver.compilationResolvers.filter { it.compilation.isMain() }

        return if (mainCompilations.isNotEmpty()) {
            //TODO[Ilya Goncharov] Hack for Mixed mode of legacy and IR tooling
            if (mainCompilations.size == 2) {
                check(
                    mainCompilations[0].compilation is KotlinJsIrCompilation
                            || mainCompilations[1].compilation is KotlinJsIrCompilation
                ) {
                    "Cannot resolve project dependency $src -> $target." +
                            "Dependency to project with multiple js compilation not supported yet."
                }
            }

            if (mainCompilations.size > 2) {
                error(
                    "Cannot resolve project dependency $src -> $target." +
                            "Dependency to project with multiple js compilation not supported yet."
                )
            }

            mainCompilations
        } else null
    }

    /**
     * Don't use directly, use [KotlinNpmResolutionManager.installIfNeeded] instead.
     */
    internal fun prepareInstallation(logger: Logger): Installation {
        synchronized(this@KotlinRootNpmResolver) {
            check(state == State.CONFIGURING) {
                "Projects must be configuring"
            }
            state = State.PROJECTS_CLOSED

            val projectResolutions = projectResolvers.values
                .map { it.close() }
                .associateBy { it.project }
            val allNpmPackages = projectResolutions.values.flatMap { it.npmProjects }

            gradleNodeModules.close()
            compositeNodeModules.close()

            nodeJs.packageManager.prepareRootProject(
                rootProject,
                nodeJs,
                rootProjectName,
                rootProjectVersion,
                logger,
                allNpmPackages,
                yarn.resolutions
                    .associate { it.path to it.toVersionString() })

            return Installation(
                projectResolutions
            )
        }
    }

    open inner class Installation(val projectResolutions: Map<String, KotlinProjectNpmResolution>) {
        operator fun get(project: String) =
            projectResolutions[project] ?: KotlinProjectNpmResolution.empty(project)

        internal fun install(
            forceUpToDate: Boolean,
            args: List<String>,
            services: ServiceRegistry,
            logger: Logger
        ): KotlinRootNpmResolution {
            synchronized(this@KotlinRootNpmResolver) {
                check(state == State.PROJECTS_CLOSED) {
                    "Projects must be closed"
                }
                state = State.INSTALLED

                val allNpmPackages = projectResolutions
                    .values
                    .flatMap { it.npmProjects }

                nodeJs.packageManager.resolveRootProject(
                    services,
                    logger,
                    nodeJs,
                    yarn.requireConfigured().home,
                    allNpmPackages,
                    args
                )

                return KotlinRootNpmResolution(rootProject, projectResolutions)
            }
        }

        internal fun closePlugins(resolution: KotlinRootNpmResolution) {
            plugins.forEach {
                it.close(resolution)
            }
        }
    }

    private fun removeOutdatedPackages(nodeJs: NodeJsRootExtension, allNpmPackages: List<KotlinCompilationNpmResolution>) {
        val packages = allNpmPackages.mapTo(mutableSetOf()) { it.npmProject.name }
        nodeJs.projectPackagesDir.listFiles()?.forEach {
            if (it.name !in packages) {
                it.deleteRecursively()
            }
        }
    }
}

const val PACKAGE_JSON_UMBRELLA_TASK_NAME = "packageJsonUmbrella"