/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*
import java.nio.file.Files

/**
 * Generates `package.json` file for [NpmProject] with npm or js dependencies and
 * runs selected [NodeJsRootExtension.packageManager] to download and install all of it's dependencies.
 *
 * All [NpmDependency] for configurations related to kotlin/js will be added to `package.json`.
 * For external gradle modules, fake npm packages will be created and added to `package.json`
 * as path to directory.
 */
internal class NpmResolver private constructor(val rootProject: Project) : AutoCloseable {
    companion object {
        fun resolveIfNeeded(project: Project): ResolutionCallResult {
            val rootProject = project.rootProject
            val process = ProjectData[rootProject]

            if (process != null && process.visit == null) return AlreadyInProgress

            val resolved = process?.visit

            return if (resolved != null) AlreadyResolved(resolved)
            else {
                run(rootProject)
                ResolvedNow(ProjectData[project]!!.visit!!)
            }
        }

        private fun run(rootProject: Project) {
            NpmResolver(rootProject).resolve()
        }

        fun getAlreadyResolvedOrNull(project: Project): ResolutionCallResult? {
            val rootProject = project.rootProject
            val process = ProjectData[rootProject]
            if (process != null && process.visit == null) return AlreadyInProgress
            val resolved = process?.visit
            if (resolved != null) return AlreadyResolved(ProjectData[project]!!.visit!!)

            return null
        }

        fun requireResolved(project: Project, reason: String = ""): NpmProjects =
            ProjectData[project]?.visit ?: error("NPM dependencies should be resolved $reason")

        fun checkRequiredDependencies(project: Project, target: RequiresNpmDependencies) {
            val requestedTaskDependencies = requireResolved(project, "before $target execution").taskRequirements
            val targetRequired = requestedTaskDependencies[target]?.toSet() ?: setOf()

            target.requiredNpmDependencies.forEach {
                check(it in targetRequired) {
                    "$it required by $target was not found resolved at the time of nodejs package manager call. " +
                            "This may be caused by changing $target configuration after npm dependencies resolution."
                }
            }
        }
    }

    sealed class ResolutionCallResult {
        object AlreadyInProgress : ResolutionCallResult()
        class AlreadyResolved(val resolution: NpmProjects) : ResolutionCallResult()
        class ResolvedNow(val resolution: NpmProjects) : ResolutionCallResult()
    }

    val gradleNodeModules = GradleNodeModulesCache(rootProject)
    val nodeJs = NodeJsPlugin.apply(rootProject).root
    val packageManager
        get() = nodeJs.packageManager

    private val allNpmPackages = mutableListOf<NpmProjectPackage>()

    class ProjectData(var visit: NpmProjects? = null) {
        companion object {
            private const val KEY = "npmResolverData"
            operator fun get(project: Project) = project.extensions.findByName(KEY) as ProjectData?
            operator fun set(project: Project, value: ProjectData) = project.extensions.add(KEY, value)
            fun getOrPut(project: Project) = this[project] ?: ProjectData().also { this[project] = it }
        }
    }

    fun resolve() {
        resolve(rootProject)
        removeOutdatedPackages()

        if (allNpmPackages.any { it.npmDependencies.isNotEmpty() }) {
            packageManager.resolveRootProject(rootProject, allNpmPackages)
        } else if (allNpmPackages.any { it.hasNodeModulesDependentTasks }) {
            NpmSimpleLinker(rootProject).link(allNpmPackages)
        }

        close()
    }

    private fun removeOutdatedPackages() {
        val packages = allNpmPackages.mapTo(mutableSetOf()) { it.npmProject.name }
        nodeJs.projectPackagesDir.listFiles()?.forEach {
            if (it.name !in packages) {
                it.deleteRecursively()
            }
        }
    }

    private fun getOrResolve(project: Project): NpmProjects {
        return ProjectData[project]?.visit ?: resolve(project)
    }

    private fun resolve(project: Project): NpmProjects {
        val data = ProjectData.getOrPut(project)

        project.subprojects.forEach {
            getOrResolve(it)
        }

        val visited = NpmProjectVisitor(this, project).visitProject()
        data.visit = visited

        allNpmPackages.addAll(visited.npmProjects)

        return visited
    }

    fun findDependentResolvedNpmProject(src: Project, target: Project): NpmProjectPackage? {
        // todo: proper finding using KotlinTargetComponent.findUsageContext
        val resolvedTarget = getOrResolve(target)
        val mainCompilations = resolvedTarget.npmProjectsByCompilation.entries.filter { it.key.name == KotlinCompilation.MAIN_COMPILATION_NAME }

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

    fun chooseVersion(oldVersion: String?, newVersion: String): String =
        oldVersion ?: newVersion // todo: real versions conflict resolution

    override fun close() {
        gradleNodeModules.close()
    }
}