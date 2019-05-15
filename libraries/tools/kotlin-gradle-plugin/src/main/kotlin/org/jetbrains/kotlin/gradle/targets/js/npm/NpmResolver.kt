/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

/**
 * Generates `package.json` file for projects with npm or js dependencies and
 * runs selected [NodeJsRootExtension.packageManager] to download and install all of it's dependencies.
 *
 * All [NpmDependency] for configurations related to kotlin/js will be added to `package.json`.
 * For external gradle modules, fake npm packages will be created and added to `package.json`
 * as path to directory.
 */
internal class NpmResolver private constructor(val rootProject: Project) : AutoCloseable {
    companion object {
        fun resolve(project: Project): ResolutionCallResult {
            val rootProject = project.rootProject
            val process = ProjectData[rootProject]

            if (process != null && process.resolved == null) return AlreadyInProgress

            val resolved = process?.resolved

            return if (resolved != null) AlreadyResolved(ProjectData[project]!!.resolved!!)
            else {
                val resolver = NpmResolver(rootProject)
                resolver.resolve(rootProject)!!
                resolver.close()
                ResolvedNow(ProjectData[project]!!.resolved!!)
            }
        }

        fun getAlreadyResolvedOrNull(project: Project): ResolutionCallResult? {
            val rootProject = project.rootProject
            val process = ProjectData[rootProject]
            if (process != null && process.resolved == null) return AlreadyInProgress
            val resolved = process?.resolved
            if (resolved != null) return AlreadyResolved(ProjectData[project]!!.resolved!!)

            return null
        }

        fun requireResolved(project: Project, reason: String = ""): ResolvedProject =
            ProjectData[project.rootProject]?.resolved
                ?: error("NPM dependencies should be resolved$reason")

        fun checkRequiredDependencies(project: Project, target: RequiresNpmDependencies) {
            val required = requireResolved(project, "before $target execution").taskDependencies
            val targetRequired = required[target]?.toSet() ?: setOf()

            target.requiredNpmDependencies.forEach {
                check(it in targetRequired) {
                    "$it required by $target after npm dependencies was resolved. " +
                            "This may be caused by changing $target configuration after npm dependencies resolution."
                }
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
    private val gradleNodeModules = GradleNodeModulesCache(rootProject)
    private val packageManager = nodeJs.packageManager
    private val npmPackages = mutableListOf<NpmPackage>()
    private val requiredByTasks = mutableMapOf<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>()
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

    class ResolvedProject(
        val npmPackage: NpmPackage?,
        val taskDependencies: Map<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>
    )

    class NpmPackage(
        val project: Project,
        val packageJson: PackageJson,
        val npmDependencies: Set<NpmDependency>
    ) {
        fun savePackageJson(gson: Gson) {
            val packageJsonFile = NpmProject[project].packageJsonFile
            packageJsonFile.ensureParentDirsCreated()
            packageJsonFile.writer().use {
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

    private fun getOrResolve(project: Project): ResolvedProject {
        return (ProjectData[project] ?: resolve(project)!!).resolved!!
    }

    private fun resolve(project: Project): ProjectData? {
        val result = ProjectData().also {
            ProjectData[project] = it
        }

        project.subprojects.forEach {
            getOrResolve(it)
        }

        val npmPackage = extractNpmPackage(project)
        if (npmPackage != null) {
            npmPackage.savePackageJson(gson)

            requirePackageManagerSetup()
            packageManager.resolveProject(npmPackage)

            npmPackages.add(npmPackage)
        }

        result.resolved = ResolvedProject(npmPackage, requiredByTasks)

        if (project == rootProject) {
            if (npmPackages.isNotEmpty()) {
                requirePackageManagerSetup()
                packageManager.resolveRootProject(rootProject, npmPackages)
            }
        }

        return result
    }

    private fun extractNpmPackage(project: Project): NpmPackage? {
        val packageJson = PackageJson(project.name, project.version.toString())
        val npmDependencies = mutableSetOf<NpmDependency>()
        val gradleDeps = NpmProjectGradleDeps()

        visitTasksRequiredDependencies(project, npmDependencies, gradleDeps)

        collectDependencies(project, npmDependencies, gradleDeps, packageJson)

        gradleDeps.externalModules.forEach {
            val relativePath = it.path.relativeTo(NpmProject[project].nodeWorkDir)
            packageJson.dependencies[it.name] = "file:$relativePath"
        }

        gradleDeps.internalModules.forEach {
            val npmPackage = getOrResolve(it).npmPackage
            if (npmPackage != null) {
                packageJson.dependencies[npmPackage.packageJson.name] = npmPackage.packageJson.version
            }
        }

        project.nodeJs.packageJsonHandlers.forEach {
            it(packageJson)
        }

        val packageJsonRequired =
            if (project == rootProject) packageManager.hookRootPackage(rootProject, packageJson, npmPackages)
            else false

        return if (!packageJsonRequired && packageJson.empty) null
        else NpmPackage(project, packageJson, npmDependencies)
    }

    private fun visitTasksRequiredDependencies(
        project: Project,
        npmDependencies: MutableSet<NpmDependency>,
        gradleDeps: NpmProjectGradleDeps
    ) {
        val requiredDependencies = mutableListOf<Dependency>()

        project.tasks.toList().forEach { task ->
            if (task.enabled && task is RequiresNpmDependencies) {
                val list = task.requiredNpmDependencies.toList()

                requiredByTasks[task] = list
                list.forEach { requiredDependency ->
                    requiredDependencies.add(requiredDependency.createDependency(project))
                }
            }
        }

        if (requiredDependencies.isNotEmpty()) {
            val configuration = project.configurations.create("jsTools")
            requiredDependencies.forEach {
                configuration.dependencies.add(it)
            }
            configuration.resolve()
            visitConfiguration(configuration, npmDependencies, gradleDeps)
        }
    }

    private fun collectDependencies(
        project: Project,
        npmDependencies: MutableSet<NpmDependency>,
        gradleDeps: NpmProjectGradleDeps,
        packageJson: PackageJson
    ) {
        val kotlin = project.kotlinExtensionOrNull

        if (kotlin != null) {
            when (kotlin) {
                is KotlinSingleTargetExtension -> visitTarget(kotlin.target, project, npmDependencies, gradleDeps, packageJson)
                is KotlinMultiplatformExtension -> kotlin.targets.forEach {
                    visitTarget(it, project, npmDependencies, gradleDeps, packageJson)
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
        gradleDeps: NpmProjectGradleDeps,
        packageJson: PackageJson
    ) {
        if (target.platformType == KotlinPlatformType.js) {
            target.compilations.toList().forEach { compilation ->
                compilation.relatedConfigurationNames.forEach {
                    val configuration = project.configurations.getByName(it)
                    visitConfiguration(configuration, npmDependencies, gradleDeps)
                }

                if (compilation is KotlinJsCompilation) {
                    visitKotlinJsCompilation(project, compilation)

                    if (compilation.name == "main") {
                        packageJson.main = compilation.compileKotlinTask.outputFile.name
                    }
                }
            }
        }
    }

    private fun visitConfiguration(
        configuration: Configuration,
        npmDependencies: MutableSet<NpmDependency>,
        gradleDeps: NpmProjectGradleDeps
    ) {
        gradleNodeModules.collectDependenciesFromConfiguration(configuration, gradleDeps)

        configuration.allDependencies.forEach { dependency ->
            when (dependency) {
                is NpmDependency -> npmDependencies.add(dependency)
            }
        }
    }

    private fun visitKotlinJsCompilation(
        project: Project,
        compilation: KotlinJsCompilation
    ) {
        val npmProject = project.npmProject
        val kotlin2JsCompile = compilation.compileKotlinTask
        if (npmProject.compileOutputCopyDest != null) {
            project.whenEvaluated {
                if (kotlin2JsCompile.outputFile.exists()) {
                    visitCompile(project.npmProject, kotlin2JsCompile)
                }
            }

            kotlin2JsCompile.doLast {
                visitCompile(project.npmProject, kotlin2JsCompile)
            }
        }
    }

    private fun visitCompile(npmProject: NpmProject, kotlin2JsCompile: Kotlin2JsCompile) {
        npmProject.project.copy { copy ->
            copy.from(kotlin2JsCompile.outputFile)
            copy.from(kotlin2JsCompile.outputFile.path + ".map")
            copy.into(npmProject.compileOutputCopyDest!!)
            copy.withSourceMapRewriter(npmProject)
        }
    }

    private fun CopySpec.withSourceMapRewriter(npmProject: NpmProject) {
        eachFile {
            if (it.name.endsWith(".js.map")) {
                it.filter(
                    mapOf(
                        "srcSourceRoot" to it.file.parentFile,
                        "targetSourceRoot" to npmProject.compileOutputCopyDest!!
                    ),
                    RewriteSourceMapFilterReader::class.java
                )
            }
        }
    }

    override fun close() {
        gradleNodeModules.close()
    }
}