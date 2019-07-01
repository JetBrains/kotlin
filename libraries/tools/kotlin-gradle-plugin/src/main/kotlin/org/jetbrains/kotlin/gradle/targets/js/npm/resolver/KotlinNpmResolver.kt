/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.NpmProjectPackage

/**
 * Generates `package.json` file for [NpmProject] with npm or js dependencies and
 * runs selected [NodeJsRootExtension.packageManager] to download and install all of it's dependencies.
 *
 * All [NpmDependency] for configurations related to kotlin/js will be added to `package.json`.
 * For external gradle modules, fake npm packages will be created and added to `package.json`
 * as path to directory.
 */
internal class KotlinNpmResolver private constructor(val rootProject: Project) {
    companion object {
        private sealed class State {
            class Resolving(val resolver: KotlinNpmResolver) : State()
            class Resolved(val resolved: KotlinNpmResolution) : State()
        }

        @Volatile
        private var initializedRootProject: Project? = null
        private var state: State? = null

        private fun getState(rootProject: Project): State {
            check(rootProject == rootProject.rootProject)

            if (initializedRootProject == null) {
                synchronized(this) {
                    if (initializedRootProject == null) {
                        initializedRootProject = rootProject
                        state = State.Resolving(KotlinNpmResolver(rootProject))
                    }
                }
            }

            check(initializedRootProject == rootProject)

            return state!!
        }

        private fun setResolution(resolution: KotlinNpmResolution) {
            state = State.Resolved(resolution)
        }

        fun getResolver(rootProject: Project): KotlinNpmResolver =
            (getState(rootProject) as? State.Resolving ?: error("NPM Dependencies already resolved and installed"))
                .resolver

        fun resolveIfNeeded(project: Project): KotlinProjectNpmResolution {
            val state = getState(project.rootProject)
            val resolution = when (state) {
                is State.Resolved -> state.resolved
                is State.Resolving -> state.resolver.installAndClose()
            }
            return resolution[project]
        }

        fun getAlreadyResolvedOrNull(project: Project): KotlinProjectNpmResolution? {
            val state = getState(project.rootProject)
            return when (state) {
                is State.Resolved -> state.resolved[project]
                is State.Resolving -> null
            }
        }

        fun requireResolved(project: Project, reason: String = ""): KotlinProjectNpmResolution =
            getAlreadyResolvedOrNull(project) ?: error("NPM dependencies should be resolved $reason")

        fun checkRequiredDependencies(project: Project, target: RequiresNpmDependencies) {
            val requestedTaskDependencies = requireResolved(
                project,
                "before $target execution"
            ).taskRequirements
            val targetRequired = requestedTaskDependencies[target]?.toSet() ?: setOf()

            target.requiredNpmDependencies.forEach {
                check(it in targetRequired) {
                    "$it required by $target was not found resolved at the time of nodejs package manager call. " +
                            "This may be caused by changing $target configuration after npm dependencies resolution."
                }
            }
        }
    }

    val gradleNodeModules = GradleNodeModulesCache(rootProject)
    val projectResolvers = mutableMapOf<Project, KotlinProjectNpmResolver>()

    init {
        if (rootProject.logger.isDebugEnabled) {
            rootProject.logger.debug("KotlinNpmResolver initialization triggered at: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        }

        rootProject.allprojects.toList().forEach {
            projectResolvers[it] = KotlinProjectNpmResolver(it, this)
        }
    }

    fun installAndClose(): KotlinNpmResolution {
        if (rootProject.logger.isDebugEnabled) {
            rootProject.logger.debug("KotlinNpmResolver installation triggered at: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        }

        val nodeJs = NodeJsPlugin.apply(rootProject).root
        val packageManager = nodeJs.packageManager
        val allNpmPackages = projectResolvers.values.flatMap { it.byCompilation.values.map { it.projectPackage } }

        removeOutdatedPackages(nodeJs, allNpmPackages)
        gradleNodeModules.close()

        if (allNpmPackages.any { it.npmDependencies.isNotEmpty() }) {
            packageManager.resolveRootProject(rootProject, allNpmPackages)
        } else if (projectResolvers.values.any { it.hasNodeModulesDependentTasks }) {
            NpmSimpleLinker(rootProject).link(allNpmPackages)
        }

        val resolution = KotlinNpmResolution(projectResolvers.values.map { it.toResolved() }.associateBy { it.project })
        setResolution(resolution)
        return resolution
    }

    private fun removeOutdatedPackages(nodeJs: NodeJsRootExtension, allNpmPackages: List<NpmProjectPackage>) {
        val packages = allNpmPackages.mapTo(mutableSetOf()) { it.npmProject.name }
        nodeJs.projectPackagesDir.listFiles()?.forEach {
            if (it.name !in packages) {
                it.deleteRecursively()
            }
        }
    }

    fun findDependentResolver(src: Project, target: Project): KotlinCompilationNpmResolver? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val targetResolver = getResolver(target.rootProject).projectResolvers[target] ?: return null
        val mainCompilations =
            targetResolver.byCompilation.entries.filter { it.key.name == KotlinCompilation.MAIN_COMPILATION_NAME }

        return if (mainCompilations.isNotEmpty()) {
            if (mainCompilations.size > 1) {
                error(
                    "Cannot resolve project dependency $src -> $target." +
                            "Dependency to project with multiple js compilation not supported yet."
                )
            }

            mainCompilations.single().value
        } else null
    }
}