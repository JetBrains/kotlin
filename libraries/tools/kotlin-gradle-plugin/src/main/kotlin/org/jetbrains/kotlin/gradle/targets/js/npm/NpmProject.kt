/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

open class NpmProject(
    val project: Project,
    val nodeWorkDir: File,
    val searchInParents: Boolean
) {
    val nodeModulesDir
        get() = nodeWorkDir.resolve(NODE_MODULES)

    val packageJsonFile: File
        get() = nodeWorkDir.resolve(PACKAGE_JSON)

    open val compileOutputCopyDest: File?
        get() = nodeModulesDir

    private val modules = object : NpmProjectModules(nodeWorkDir, nodeModulesDir) {
        override val parent get() = if (searchInParents) parentModules else null
    }

    private val parentModules: NpmProjectModules?
        get() = project.parent?.npmProject?.modules

    val hoistGradleNodeModules: Boolean
        get() = project.nodeJs.root.packageManager.shouldHoistGradleNodeModules(project)

    val gradleNodeModulesDir: File
        get() =
            if (hoistGradleNodeModules) project.rootProject.npmProject.nodeModulesDir
            else nodeModulesDir

    open fun compileOutput(compilationTask: Kotlin2JsCompile): File {
        return compileOutputCopyDest?.resolve(compilationTask.outputFile.name) ?: compilationTask.outputFile
    }

    fun useTool(exec: ExecSpec, tool: String, vararg args: String) {
        exec.workingDir = nodeWorkDir
        exec.executable = project.nodeJs.root.environment.nodeExecutable
        exec.args = listOf(require(tool)) + args
    }

    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    fun require(request: String): String {
        NpmResolver.requireResolved(project)
        return modules.require(request)
    }

    /**
     * Find node module according to https://nodejs.org/api/modules.html#modules_all_together,
     * with exception that instead of traversing parent folders, we are traversing parent projects
     */
    internal fun resolve(name: String): File? = modules.resolve(name)

    override fun toString() = "NpmProject($nodeWorkDir)"

    companion object {
        const val PACKAGE_JSON = "package.json"
        const val NODE_MODULES = "node_modules"

        operator fun get(project: Project): NpmProject {
            val nodeJsRootExtension = NodeJsPlugin.apply(project)

            return nodeJsRootExtension.root.layout.newNpmProject(project)
        }
    }
}

val Project.npmProject
    get() = NpmProject[this]