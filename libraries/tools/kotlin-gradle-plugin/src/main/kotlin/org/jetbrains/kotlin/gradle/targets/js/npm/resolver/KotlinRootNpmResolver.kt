/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.dukat.DukatRootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution

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
        if (nodeJs.experimental.generateKotlinExternals) {
            it.add(DukatRootResolverPlugin(this))
        }
    }

    private var closed: Boolean = false

    val gradleNodeModules = GradleNodeModulesCache(nodeJs)
    private val projectResolvers = mutableMapOf<Project, KotlinProjectNpmResolver>()

    fun alreadyResolvedMessage(action: String) = "Cannot $action. NodeJS projects already resolved."

    @Synchronized
    fun addProject(target: Project) {
        check(!closed) { alreadyResolvedMessage("add new project: $target") }
        projectResolvers[target] = KotlinProjectNpmResolver(target, this)
    }

    operator fun get(project: Project) = projectResolvers[project] ?: error("$project is not configured for JS usage")

    val compilations: Collection<KotlinJsCompilation>
        get() = projectResolvers.values.flatMap { it.compilationResolvers.map { it.compilation } }

    fun findDependentResolver(src: Project, target: Project): KotlinCompilationNpmResolver? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val targetResolver = this[target]
        val mainCompilations = targetResolver.compilationResolvers.filter { it.compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME }

        return if (mainCompilations.isNotEmpty()) {
            if (mainCompilations.size > 1) {
                error(
                    "Cannot resolve project dependency $src -> $target." +
                            "Dependency to project with multiple js compilation not supported yet."
                )
            }

            mainCompilations.single()
        } else null
    }

    /**
     * Don't use directly, use [NodeJsRootExtension.resolveIfNeeded] instead.
     */
    internal fun close(forceUpToDate: Boolean): KotlinRootNpmResolution {
        check(!closed)
        closed = true

        val projectResolutions = projectResolvers.values
            .map { it.close() }
            .associateBy { it.project }
        val allNpmPackages = projectResolutions.values.flatMap { it.npmProjects }

        gradleNodeModules.close()

        // we need manual up-to-date checking to avoid call package manager during
        // idea import if nothing was changed
        // we should call it even kotlinNpmInstall task is up-to-date (skipPackageManager is true)
        // because our upToDateChecks saves state for next execution
        val upToDateChecks = allNpmPackages.map {
            PackageJsonUpToDateCheck(it.npmProject)
        }
        val upToDate = forceUpToDate || upToDateChecks.all { it.upToDate }

        if (allNpmPackages.any { it.externalNpmDependencies.isNotEmpty() }) {
            nodeJs.packageManager.resolveRootProject(rootProject, allNpmPackages, upToDate)
        } else if (projectResolvers.values.any { it.taskRequirements.hasNodeModulesDependentTasks }) {
            if (!upToDate) {
                NpmSimpleLinker(nodeJs).link(allNpmPackages)
            }
        }

        upToDateChecks.forEach { it.commit() }

        return KotlinRootNpmResolution(rootProject, projectResolutions)
    }

    internal fun closePlugins(resolution: KotlinRootNpmResolution) {
        plugins.forEach {
            it.close(resolution)
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