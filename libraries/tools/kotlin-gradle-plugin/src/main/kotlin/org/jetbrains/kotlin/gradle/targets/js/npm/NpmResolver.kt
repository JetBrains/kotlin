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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*

/**
 * Generates `package.json` file for projects with npm or js dependencies and
 * runs selected [NodeJsRootExtension.packageManager] to download and install it.
 *
 * All [NpmDependency] for configurations related to kotlin/js will be added to `package.json`.
 * For external gradle modules, fake npm packages will be created and added to `package.json`
 * as path to directory.
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
            else {
                val resolver = NpmResolver(rootProject)
                check(resolver.resolve(rootProject, null))
                ResolvedNow(ResolvedProject(resolver.npmPackages))
            }
        }
    }

    sealed class ResolutionCallResult {
        object AlreadyInProgress : ResolutionCallResult()
        class AlreadyResolved(val resolution: ResolvedProject) : ResolutionCallResult()
        class ResolvedNow(val resolution: ResolvedProject) : ResolutionCallResult()
    }

    private val nodeJs = NodeJsPlugin.apply(rootProject).root
    private val npmProject = rootProject.npmProject
    private val packageManager = nodeJs.packageManager
    private val hoistGradleNodeModules = npmProject.hoistGradleNodeModules
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

    class ResolvedProject(val npmPackages: Collection<NpmPackage>) {
        val dependencies by lazy {
            npmPackages.flatMapTo(mutableSetOf()) { it.npmDependencies }
        }
    }

    class NpmPackage(
        val project: Project,
        val packageJson: PackageJson,
        val npmDependencies: Set<NpmDependency>
    ) {
        fun savePackageJson(gson: Gson) {
            NpmProject[project].packageJsonFile.writer().use {
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
        parentGradleModules: GradleNodeModules?
    ): Boolean {
        val existedProcess = ProjectData[project]
        if (existedProcess != null) {
            if (existedProcess.resolved != null) error("yarn dependencies for $project already resolved")
            else {
                // called inside resolution of classpath (from visitTarget)
                // we should return as we are already in progress
                return false
            }
        } else ProjectData().also {
            ProjectData[project] = it
        }

        val gradleModules =
            if (hoistGradleNodeModules && parentGradleModules != null) parentGradleModules
            else GradleNodeModules(project)

        project.subprojects.forEach {
            resolve(it, gradleModules)
        }

        val npmPackage = extractNpmPackage(project, gradleModules)
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

        if (gradleModules != parentGradleModules) {
            gradleModules.close()
        }

        return true
    }

    private fun extractNpmPackage(
        project: Project,
        gradleComponents: GradleNodeModules
    ): NpmPackage? {
        val packageJson = PackageJson(project.name, project.version.toString())
        val npmDependencies = mutableSetOf<NpmDependency>()

        visitNpmDependencies(project, npmDependencies, gradleComponents, packageJson)

        if (!hoistGradleNodeModules || project == project.rootProject) {
            gradleComponents.modules.forEach {
                val relativePath = it.path.relativeTo(NpmProject[project].nodeWorkDir)
                packageJson.dependencies[it.name] = "file:$relativePath"
            }
        }

        project.nodeJs.packageJsonHandlers.forEach {
            it(packageJson)
        }

        val packageJsonRequired = if (project == rootProject) {
            packageManager.hookRootPackage(rootProject, packageJson, npmPackages)
        } else false

        return if (packageJsonRequired || !packageJson.empty) NpmPackage(project, packageJson, npmDependencies) else null
    }

    private fun visitNpmDependencies(
        project: Project,
        npmDependencies: MutableSet<NpmDependency>,
        gradleComponents: GradleNodeModules,
        packageJson: PackageJson
    ) {
        val kotlin = project.kotlinExtensionOrNull

        if (kotlin != null) {
            when (kotlin) {
                is KotlinSingleTargetExtension -> visitTarget(kotlin.target, project, npmDependencies, gradleComponents)
                is KotlinMultiplatformExtension -> kotlin.targets.forEach {
                    visitTarget(it, project, npmDependencies, gradleComponents)
                }
            }

            npmDependencies.forEach {
                packageJson.dependencies[it.key] = chooseVersion(packageJson.dependencies[it.key], it.version)
            }
        }
    }

    private fun chooseVersion(oldVersion: String?, newVersion: String): String =
        oldVersion ?: newVersion // todo: real versions conflict resolution

    private fun visitTarget(
        target: KotlinTarget,
        project: Project,
        npmDependencies: MutableSet<NpmDependency>,
        gradleComponents: GradleNodeModules
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

                gradleComponents.visitCompilation(compilation)
            }
        }
    }
}