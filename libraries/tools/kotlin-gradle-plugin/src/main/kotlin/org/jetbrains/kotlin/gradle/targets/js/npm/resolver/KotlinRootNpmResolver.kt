/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dukat.DukatRootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.CompositeNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonUpToDateCheck
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.toVersionString
import org.jetbrains.kotlin.gradle.tasks.registerTask

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
internal class KotlinRootNpmResolver internal constructor(
    val nodeJs: NodeJsRootExtension,
    val forceFullResolve: Boolean
) {
    val rootProject: Project
        get() = nodeJs.rootProject

    val plugins = mutableListOf<RootResolverPlugin>().also {
        it.add(DukatRootResolverPlugin(this))
    }

    enum class State {
        CONFIGURING,
        PROJECTS_CLOSED,
        INSTALLED
    }

    @Volatile
    private var state: State = State.CONFIGURING

    val gradleNodeModules = GradleNodeModulesCache(nodeJs)
    val compositeNodeModules = CompositeNodeModulesCache(nodeJs)
    val projectResolvers = mutableMapOf<String, KotlinProjectNpmResolver>()

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
    internal fun prepareInstallation(): Installation {
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

            val yarn = YarnPlugin.apply(rootProject)

            nodeJs.packageManager.prepareRootProject(
                rootProject,
                allNpmPackages,
                yarn.resolutions
                    .associate { it.path to it.toVersionString() }
            )

            return Installation(
                projectResolutions
            )
        }
    }

    open inner class Installation(val projectResolutions: Map<Project, KotlinProjectNpmResolution>) {
        operator fun get(project: Project) =
            projectResolutions[project] ?: KotlinProjectNpmResolution.empty(project)

        internal fun install(
            forceUpToDate: Boolean,
            args: List<String>
        ): KotlinRootNpmResolution {
            synchronized(this@KotlinRootNpmResolver) {
                check(state == State.PROJECTS_CLOSED) {
                    "Projects must be closed"
                }
                state = State.INSTALLED

                val allNpmPackages = projectResolutions
                    .values
                    .flatMap { it.npmProjects }

                // we need manual up-to-date checking to avoid call package manager during
                // idea import if nothing was changed
                // we should call it even kotlinNpmInstall task is up-to-date (skipPackageManager is true)
                // because our upToDateChecks saves state for next execution
                val upToDateChecks = allNpmPackages.map {
                    PackageJsonUpToDateCheck(it.npmProject)
                }
                val upToDate = forceUpToDate || upToDateChecks.all { it.upToDate }

                nodeJs.packageManager.resolveRootProject(
                    nodeJs.rootProject,
                    allNpmPackages,
                    upToDate,
                    args
                )

                upToDateChecks.forEach { it.commit() }

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