/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File
import java.io.Serializable

val KotlinJsIrCompilation.npmProject: NpmProject
    get() = NpmProject(this)

@Deprecated("Use npmProject for KotlinJsIrCompilation")
val KotlinJsCompilation.npmProject: NpmProject
    get() = NpmProject(this as KotlinJsIrCompilation)

/**
 * Basic info for [NpmProject] created from [compilation].
 * This class contains only basic info.
 *
 * More info can be obtained from [KotlinCompilationNpmResolution], which is available after project resolution (after [KotlinNpmInstallTask] execution).
 */
open class NpmProject(@Transient val compilation: KotlinJsIrCompilation) : Serializable {
    val compilationName = compilation.disambiguatedName

    private val extension: Provider<String> = compilation.fileExtension

    val name: Provider<String> = compilation.outputModuleName

    @delegate:Transient
    val nodeJs by lazy {
        project.rootProject.kotlinNodeJsExtension
    }

    val dir: Provider<Directory> = nodeJs.projectPackagesDirectory.zip(name) { directory, name ->
        directory.dir(name)
    }

    val target: KotlinJsTargetDsl
        get() = compilation.target as KotlinJsTargetDsl

    val project: Project
        get() = target.project

    val nodeModulesDir
        get() = dir.map { it.dir(NODE_MODULES) }

    val packageJsonFile: Provider<RegularFile>
        get() = dir.map { it.file(PACKAGE_JSON) }

    val packageJsonTaskName: String
        get() = compilation.disambiguateName("packageJson")

    val packageJsonTask: KotlinPackageJsonTask
        get() = project.tasks.getByName(packageJsonTaskName) as KotlinPackageJsonTask

    val packageJsonTaskPath: String
        get() = packageJsonTask.path

    val dist: Provider<Directory>
        get() = dir.map { it.dir(DIST_FOLDER) }

    val main: Provider<String> = extension.zip(name) { ext, name ->
        "${DIST_FOLDER}/$name.$ext"
    }

    val publicPackageJsonTaskName: String
        get() = compilation.disambiguateName(PublicPackageJsonTask.NAME)

    internal val modules by lazy {
        NpmProjectModules(dir.getFile())
    }

    private val nodeExecutable by lazy {
        nodeJs.requireConfigured().executable
    }

    fun useTool(
        exec: ExecSpec,
        tool: String,
        nodeArgs: List<String> = listOf(),
        args: List<String>
    ) {
        exec.workingDir(dir)
        exec.executable(nodeExecutable)
        exec.args = nodeArgs + require(tool) + args
    }

    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    fun require(request: String): String {
//        nodeJs.npmResolutionManager.requireAlreadyInstalled(project)
        return modules.require(request)
    }

    /**
     * Find node module according to https://nodejs.org/api/modules.html#modules_all_together,
     * with exception that instead of traversing parent folders, we are traversing parent projects
     */
    internal fun resolve(name: String): File? = modules.resolve(name)

    override fun toString() = "NpmProject(${name.get()})"

    companion object {
        const val PACKAGE_JSON = "package.json"
        const val NODE_MODULES = "node_modules"
        const val DIST_FOLDER = "kotlin"
    }
}