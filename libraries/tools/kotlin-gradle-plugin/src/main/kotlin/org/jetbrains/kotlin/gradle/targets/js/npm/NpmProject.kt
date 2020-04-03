/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import java.io.File

val KotlinJsCompilation.npmProject: NpmProject
    get() = NpmProject(this)

/**
 * Basic info for [NpmProject] created from [compilation].
 * This class contains only basic info.
 *
 * More info can be obtained from [KotlinCompilationNpmResolution], which is available after project resolution (after [KotlinNpmInstallTask] execution).
 */
open class NpmProject(val compilation: KotlinJsCompilation) {
    val name: String
        get() = buildNpmProjectName()

    val nodeJs
        get() = NodeJsRootPlugin.apply(project.rootProject)

    val dir: File
        get() = nodeJs.projectPackagesDir.resolve(name)

    val target: KotlinJsTargetDsl
        get() = compilation.target as KotlinJsTargetDsl

    val project: Project
        get() = target.project

    val nodeModulesDir
        get() = dir.resolve(NODE_MODULES)

    val packageJsonFile: File
        get() = dir.resolve(PACKAGE_JSON)

    val packageJsonTaskName: String
        get() = compilation.disambiguateName("packageJson")

    val packageJsonTask: KotlinPackageJsonTask
        get() = project.tasks.getByName(packageJsonTaskName) as KotlinPackageJsonTask

    val dist: File
        get() = dir.resolve(DIST_FOLDER)

    val main: String
        get() = "$DIST_FOLDER/$name.js"

    val externalsDirRoot: File
        get() = project.buildDir.resolve("externals").resolve(name)

    val externalsDir: File
        get() = externalsDirRoot.resolve("src")

    internal val modules = NpmProjectModules(dir)

    private val rootNodeModules: NpmProjectModules?
        get() = NpmProjectModules(nodeJs.rootPackageDir)

    fun useTool(exec: ExecSpec, tool: String, vararg args: String) {
        exec.workingDir = dir
        exec.executable = nodeJs.requireConfigured().nodeExecutable
        exec.args = listOf(require(tool)) + args
    }

    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    fun require(request: String): String {
        nodeJs.npmResolutionManager.requireAlreadyInstalled(project)
        return modules.require(request)
    }

    /**
     * Find node module according to https://nodejs.org/api/modules.html#modules_all_together,
     * with exception that instead of traversing parent folders, we are traversing parent projects
     */
    internal fun resolve(name: String): File? = modules.resolve(name)

    private fun buildNpmProjectName(): String {
        compilation.outputModuleName?.let {
            return it
        }

        val project = target.project

        val moduleName = target.moduleName

        val compilationName = if (compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME) {
            compilation.name
        } else null

        if (moduleName != null) {
            return sequenceOf(moduleName, compilationName)
                .filterNotNull()
                .joinToString("-")
        }

        val rootProjectName = project.rootProject.name

        val localName = if (project != project.rootProject) {
            project.name
        } else null

        val targetName = if (target.name.isNotEmpty() && target.name.toLowerCase() != "js") {
            target.name
        } else null

        return sequenceOf(
            rootProjectName,
            localName,
            targetName,
            compilationName
        )
            .filterNotNull()
            .joinToString("-")
    }

    override fun toString() = "NpmProject($name)"

    companion object {
        const val PACKAGE_JSON = "package.json"
        const val NODE_MODULES = "node_modules"
        const val DIST_FOLDER = "kotlin"
    }
}