/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dukat.DukatRootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnEnv
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.toVersionString
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.gradle.utils.FileSystemOperationsCompat
import org.jetbrains.kotlin.gradle.utils.unavailableValueError

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 *
 * This class contains many transient properties to deduplicate data when configuration cache is used.
 * Regularly tasks share the same instance of this class, but with configuration cache each task that holds a reference to the instance will
 * create an own copy. We use build services as a single storage for the heavy state of this class.
 */
internal class KotlinRootNpmResolver internal constructor(
    @Transient
    val nodeJs: NodeJsRootExtension?,
    val forceFullResolve: Boolean
) {
    private val nodeJs_
        get() = nodeJs ?: unavailableValueError("nodeJs")

    private val rootProject: Project?
        get() = nodeJs?.rootProject

    private val rootProject_: Project
        get() = rootProject ?: unavailableValueError("rootProject")

    private val rootProjectName by lazy {
        rootProject_.name
    }

    private val rootProjectVersion by lazy {
        rootProject_.version.toString()
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

    private val archiveOperations by lazy { ArchiveOperationsCompat(rootProject_) }
    private val fs by lazy { FileSystemOperationsCompat(rootProject_) }

    internal val gradleNodeModulesProvider: Provider<GradleNodeModulesCache> =
        rootProject_
            .gradle.sharedServices.registerIfAbsent("gradle-node-modules", GradleNodeModulesCache::class.java) {
                it.parameters.cacheDir.set(nodeJs_.nodeModulesGradleCacheDir)
                it.parameters.rootProjectDir.set(rootProject_.projectDir)
            }

    val gradleNodeModules: GradleNodeModulesCache
        get() = gradleNodeModulesProvider.get().also {
            it.archiveOperations = archiveOperations
            it.fs = fs
        }

    internal val compositeNodeModulesProvider: Provider<CompositeNodeModulesCache> =
        rootProject_
            .gradle.sharedServices.registerIfAbsent("composite-node-modules", CompositeNodeModulesCache::class.java) {
                it.parameters.cacheDir.set(nodeJs_.nodeModulesGradleCacheDir)
                it.parameters.rootProjectDir.set(rootProject_.projectDir)
            }

    val compositeNodeModules: CompositeNodeModulesCache
        get() = compositeNodeModulesProvider.get()

    @Transient
    private val plugins_: MutableList<RootResolverPlugin>? = mutableListOf<RootResolverPlugin>().also {
        it.add(DukatRootResolverPlugin(forceFullResolve))
    }

    @Transient
    private val projectResolvers_: MutableMap<String, KotlinProjectNpmResolver>? = mutableMapOf()

    @Transient
    private val yarnEnvironment_: Provider<YarnEnv>? = rootProject_.provider {
        YarnPlugin.apply(rootProject_).requireConfigured()
    }

    @Transient
    private val npmEnvironment_: Provider<NpmEnvironment>? = rootProject_.provider {
        nodeJs_.asNpmEnvironment
    }

    @Transient
    private val yarnResolutions_: Provider<List<YarnResolution>>? = rootProject_.provider {
        YarnPlugin.apply(rootProject_).resolutions
    }

    @Transient
    private val taskRequirements_: TasksRequirements? = nodeJs_.taskRequirements

    private val resolverStateHolder by lazy {
        rootProject_.gradle.sharedServices.registerIfAbsent(
            KotlinRootNpmResolverStateHolder::class.qualifiedName,
            KotlinRootNpmResolverStateHolder::class.java
        ) { service ->
            service.parameters.plugins.set(plugins_)
            service.parameters.projectResolvers.set(projectResolvers_)
            service.parameters.packageManager.set(nodeJs_.packageManager)
            service.parameters.yarnEnvironment.set(yarnEnvironment_?.get())
            service.parameters.npmEnvironment.set(npmEnvironment_?.get())
            service.parameters.yarnResolutions.set(yarnResolutions_?.get())
            service.parameters.taskRequirements.set(taskRequirements_)
            service.parameters.packageJsonHandlers.set(compilations.associate { compilation ->
                "${compilation.project.path}:${compilation.disambiguatedName}" to compilation.packageJsonHandlers
            }.filter { it.value.isNotEmpty() })
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

    private val projectResolvers
        get() = projectResolvers_ ?: configurationCacheProjectResolvers

    private val packageManager
        get() = nodeJs?.packageManager ?: resolverStateHolder.get().parameters.packageManager.get()

    private val yarnEnvironment
        get() = yarnEnvironment_?.get() ?: resolverStateHolder.get().parameters.yarnEnvironment.get()

    private val npmEnvironment
        get() = npmEnvironment_?.get() ?: resolverStateHolder.get().parameters.npmEnvironment.get()

    private val yarnResolutions
        get() = yarnResolutions_?.get() ?: resolverStateHolder.get().parameters.yarnResolutions.get()

    internal val taskRequirements
        get() = taskRequirements_ ?: resolverStateHolder.get().parameters.taskRequirements.get()

    internal val mayBeUpToDateTasksRegistry =
        MayBeUpToDatePackageJsonTasksRegistry.registerIfAbsent(rootProject_)

    fun alreadyResolvedMessage(action: String) = "Cannot $action. NodeJS projects already resolved."

    fun addProject(target: Project) {
        synchronized(projectResolvers) {
            check(state == State.CONFIGURING) { alreadyResolvedMessage("add new project: $target") }
            projectResolvers[target.path] = KotlinProjectNpmResolver(target, this)
        }
    }

    operator fun get(projectPath: String) = projectResolvers[projectPath] ?: error("$projectPath is not configured for JS usage")

    val compilations: Collection<KotlinJsCompilation>
        get() = projectResolvers.values.flatMap { it.compilationResolvers.map { it.compilation } }

    internal fun getPackageJsonHandlers(projectPath: String, compilationDisambiguatedName: String): List<PackageJson.() -> Unit> =
        resolverStateHolder.get().parameters.packageJsonHandlers.get()["$projectPath:$compilationDisambiguatedName"] ?: emptyList()

    fun findDependentResolver(src: Project, target: Project): List<KotlinCompilationNpmResolver>? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val targetResolver = this[target.path]
        val mainCompilations = targetResolver.compilationResolvers.filter { it.compilation.isMain() }

        if (mainCompilations.isEmpty()) return null

        //TODO[Ilya Goncharov, Igor Iakovlev] Hack for Mixed mode of legacy and IR tooling and Wasm
        var containsWasm = false
        var containsIrJs = false
        var containsLegacyJs = false
        val errorMessage = "Cannot resolve project dependency $src -> $target." +
                "Dependency to project with multiple js/wasm compilations is not supported yet."

        check(mainCompilations.size <= 3) { errorMessage }
        for (npmResolver in mainCompilations) {
            when (val compilation = npmResolver.compilation) {
                is KotlinJsIrCompilation -> {
                    if (compilation.platformType == KotlinPlatformType.wasm) {
                        check(!containsWasm) { errorMessage }
                        containsWasm = true
                    } else {
                        check(!containsIrJs) { errorMessage }
                        containsIrJs = true
                    }
                }
                else -> {
                    check(!containsLegacyJs) { errorMessage }
                    containsLegacyJs = true
                }
            }
        }
        check(containsWasm || containsIrJs || containsLegacyJs) { errorMessage }

        return mainCompilations
    }

    /**
     * Don't use directly, use [KotlinNpmResolutionManager.installIfNeeded] instead.
     */
    internal fun prepareInstallation(logger: Logger): Installation {
        synchronized(projectResolvers) {
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

            packageManager.prepareRootProject(
                rootProject,
                npmEnvironment,
                rootProjectName,
                rootProjectVersion,
                logger,
                allNpmPackages,
                yarnResolutions
                    .associate { it.path to it.toVersionString() },
                forceFullResolve
            )

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
            synchronized(projectResolvers) {
                check(state == State.PROJECTS_CLOSED) {
                    "Projects must be closed"
                }
                state = State.INSTALLED

                val allNpmPackages = projectResolutions
                    .values
                    .flatMap { it.npmProjects }

                packageManager.resolveRootProject(
                    services,
                    logger,
                    npmEnvironment,
                    yarnEnvironment,
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