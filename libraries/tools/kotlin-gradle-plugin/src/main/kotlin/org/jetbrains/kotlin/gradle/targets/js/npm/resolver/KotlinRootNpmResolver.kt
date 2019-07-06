/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModulesCache
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution

/**
 * Generates `package.json` file for [NpmProject] with npm or js dependencies and
 * runs selected [NodeJsRootExtension.packageManager] to download and install all of it's dependencies.
 *
 * All [NpmDependency] for configurations related to kotlin/js will be added to `package.json`.
 * For external gradle modules, fake npm packages will be created and added to `package.json`
 * as path to directory.
 */
internal class KotlinRootNpmResolver internal constructor(val nodeJs: NodeJsRootExtension) : NodeJsRootExtension.ResolutionStateData {
    val rootProject: Project
        get() = nodeJs.rootProject

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

    override val compilations: Collection<KotlinJsCompilation>
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
    internal fun close(): KotlinRootNpmResolution {
        check(!closed)
        closed = true

        val projectResolutions = projectResolvers.values.map { it.close() }.associateBy { it.project }
        val allNpmPackages = projectResolutions.values.flatMap { it.npmProjects }

        gradleNodeModules.close()

        val wasUpToDate = when {
            allNpmPackages.any { it.externalNpmDependencies.isNotEmpty() } -> {
                nodeJs.packageManager.resolveRootProject(rootProject, allNpmPackages) == NpmApi.Result.upToDate
            }
            projectResolvers.values.any { it.taskRequirements.hasNodeModulesDependentTasks } -> {
                NpmSimpleLinker(nodeJs).link(allNpmPackages)
                true // todo
            }
            else -> true
        }

        return KotlinRootNpmResolution(wasUpToDate, rootProject, projectResolutions)
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