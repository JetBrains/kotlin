/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*

/**
 * [NpmResolver] runs selected [NodeJsRootExtension.packageManager] on root project
 * with configured `package.json` files in all projects.
 *
 * `package.json`
 *
 * - compile(npm(...)) dependencies
 * - compile(npm(...)) dependencies from external modules
 * - compile(non npm(...)) dependencies transitively
 */
internal class NpmResolver private constructor(val rootProject: Project) {
    companion object {
        fun resolve(project: Project): ResolutionCallResult {
            val rootProject = project.rootProject
            val process = ProjectData[rootProject]

            if (process != null && process.resolved == null)
                return AlreadyInProgress

            val resolved = process?.resolved

            return if (resolved != null) AlreadyResolved(resolved)
            else ResolvedNow(NpmResolver(rootProject).resolve(rootProject, null)!!)
        }
    }

    sealed class ResolutionCallResult {
        object AlreadyInProgress : ResolutionCallResult()
        class AlreadyResolved(val resolution: ResolvedProject) : ResolutionCallResult()
        class ResolvedNow(val resolution: ResolvedProject) : ResolutionCallResult()
    }

    private val nodeJs = NodeJsPlugin[rootProject]
    private val packageManager = nodeJs.packageManager
    private val hoistGradleNodeModules = packageManager.hoistGradleNodeModules
    private val npmPackages = mutableListOf<NpmPackage>()
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    class ProjectData(var resolved: ResolvedProject? = null) {
        companion object {
            private const val KEY = "npmResolverData"
            operator fun get(project: Project) = project.extensions.findByName(KEY) as ProjectData?
            operator fun set(project: Project, value: ProjectData) = project.extensions.add(KEY, value)
        }
    }

    class ResolvedProject(val dependencies: Set<NpmDependency>)

    class NpmPackage(
        val project: Project,
        val packageJson: PackageJson,
        val npmDependencies: Set<NpmDependency>
    ) {
        fun savePackageJson(gson: Gson) {
            NpmProjectLayout[project].packageJsonFile.writer().use {
                gson.toJson(packageJson, it)
            }
        }
    }

    private var packageManagerInstalled = false

    private fun requirePackageManagerSetup() {
        if (packageManagerInstalled) return
        packageManager.setup(rootProject)
        packageManagerInstalled = true
    }

    private fun resolve(
        project: Project,
        parentGradleNodeModules: GradleNodeModulesSync?
    ): ResolvedProject? {
        val existedProcess = ProjectData[project]
        val process = if (existedProcess != null) {
            if (existedProcess.resolved != null) error("yarn dependencies for $project already resolved")
            else {
                // called inside resolution of classpath (from visitTarget)
                // we should return as we are already in progress
                return null
            }
        } else ProjectData().also {
            ProjectData[project] = it
        }

        val gradleNodeModules =
            if (hoistGradleNodeModules && parentGradleNodeModules != null) parentGradleNodeModules
            else newGradleNodeModulesSync(project)

        project.subprojects.forEach {
            resolve(it, gradleNodeModules)
        }

        val npmPackage = extractNpmPackage(project, gradleNodeModules)
        if (npmPackage != null) {
            npmPackage.savePackageJson(gson)

            requirePackageManagerSetup()
            packageManager.resolveProject(npmPackage)

            npmPackages.add(npmPackage)
        }

        if (project == rootProject) {
            if (npmPackages.isNotEmpty()) {
                requirePackageManagerSetup()
                packageManager.resolveRootProject(rootProject, npmPackages)
            }
        }

        if (gradleNodeModules != parentGradleNodeModules) {
            gradleNodeModules.sync()
        }

        val npmDependencies = npmPackage?.npmDependencies ?: setOf()
        return ResolvedProject(npmDependencies).also {
            process.resolved = it
        }
    }

    private fun newGradleNodeModulesSync(project: Project): GradleNodeModulesSync {
        return GradleNodeModulesSync(project).also {
            it.loadOldState()
        }
    }

    private fun extractNpmPackage(
        project: Project,
        gradleComponentsSync: GradleNodeModulesSync
    ): NpmPackage? {
        val packageJson = PackageJson(project.name, project.version.toString())
        val transitiveDependencies = mutableListOf<GradleNodeModulesSync.TransitiveNpmDependency>()

        val kotlin = project.kotlinExtension
        val npmDependencies = mutableSetOf<NpmDependency>()
        when (kotlin) {
            is KotlinSingleTargetExtension -> visitTarget(
                kotlin.target,
                project,
                npmDependencies,
                gradleComponentsSync,
                transitiveDependencies
            )
            is KotlinMultiplatformExtension -> kotlin.targets.forEach {
                visitTarget(it, project, npmDependencies, gradleComponentsSync, transitiveDependencies)
            }
        }

        npmDependencies.forEach {
            packageJson.dependencies[it.key] = chooseVersion(packageJson.dependencies[it.key], it.version)
        }

        transitiveDependencies.forEach {
            packageJson.dependencies[it.key] = chooseVersion(packageJson.dependencies[it.key], it.version)
        }

        NodeJsExtension[project].packageJsonHandlers.forEach {
            it(packageJson)
        }

        if (project == rootProject) {
            packageManager.hookRootPackage(rootProject, packageJson, npmPackages)
        }

        return if (!packageJson.empty) NpmPackage(project, packageJson, npmDependencies) else null
    }

    private fun chooseVersion(oldVersion: String?, newVersion: String): String =
        oldVersion ?: newVersion // todo: real versions conflict resolution

    private fun visitTarget(
        target: KotlinTarget,
        project: Project,
        npmDependencies: MutableSet<NpmDependency>,
        gradleComponentsSync: GradleNodeModulesSync,
        transitiveDependencies: MutableList<GradleNodeModulesSync.TransitiveNpmDependency>
    ) {
        if (target.platformType == KotlinPlatformType.js) {
            target.compilations.toList().forEach { compilation ->
                compilation.relatedConfigurationNames.forEach {
                    project.configurations.getByName(it).allDependencies.forEach { dependency ->
                        when (dependency) {
                            is NpmDependency -> npmDependencies.add(dependency)
                        }
                    }
                }

                gradleComponentsSync.visitCompilation(compilation, project, transitiveDependencies)
            }
        }
    }
}