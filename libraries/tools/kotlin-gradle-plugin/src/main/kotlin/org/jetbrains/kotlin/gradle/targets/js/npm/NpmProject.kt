/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.JsonObject
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

    fun useTool(exec: ExecSpec, tool: String, vararg args: String) {
        NpmResolver.resolve(project)

        exec.workingDir = nodeWorkDir
        exec.executable = project.nodeJs.root.environment.nodeExecutable
        exec.args = listOf(require(tool)) + args
    }

    val hoistGradleNodeModules: Boolean
        get() = project.nodeJs.root.packageManager.shouldHoistGradleNodeModules(project)

    val gradleNodeModulesDir: File
        get() =
            if (hoistGradleNodeModules) project.rootProject.npmProject.nodeModulesDir
            else nodeModulesDir

    open fun compileOutput(compilationTask: Kotlin2JsCompile): File {
        return compileOutputCopyDest?.resolve(compilationTask.outputFile.name) ?: compilationTask.outputFile
    }

    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    fun require(request: String): String {
        NpmResolver.requireResolved(project)
        return resolve(request)?.canonicalPath ?: error("Cannot find node module \"$request\" in \"$this\"")
    }

    /**
     * Find node module according to https://nodejs.org/api/modules.html#modules_all_together,
     * with exception that instead of traversing parent folders, we are traversing parent projects
     */
    internal fun resolve(name: String, context: File = nodeWorkDir): File? =
        if (name.startsWith("/")) resolve(name.removePrefix("/"), File("/"))
        else resolveAsRelative("./", name, context)
            ?: resolveAsRelative("/", name, context)
            ?: resolveAsRelative("../", name, context)
            ?: resolveInNodeModulesDir(name, nodeModulesDir)
            ?: if (searchInParents) project.parent?.let { NpmProject[it].resolve(name) } else null

    private fun resolveAsRelative(prefix: String, name: String, context: File): File? {
        if (!name.startsWith(prefix)) return null

        val relative = context.resolve(name.removePrefix(prefix))
        return resolveAsFile(relative)
            ?: resolveAsDirectory(relative)
    }

    private fun resolveInNodeModulesDir(name: String, nodeModulesDir: File): File? {
        return resolveAsFile(nodeModulesDir.resolve(name))
            ?: resolveAsDirectory(nodeModulesDir.resolve(name))
    }

    private fun resolveAsDirectory(dir: File): File? {
        val packageJsonFile = dir.resolve("package.json")
        val main: String? = if (packageJsonFile.isFile) {
            val packageJson = packageJsonFile.reader().use {
                Gson().fromJson(it, JsonObject::class.java)
            }

            packageJson["main"] as? String?
                ?: packageJson["module"] as? String?
                ?: packageJson["browser"] as? String?
        } else null

        return if (main != null) {
            val mainFile = dir.resolve(main)
            resolveAsFile(mainFile)
                ?: resolveIndex(mainFile)
        } else resolveIndex(dir)
    }

    private fun resolveIndex(dir: File): File? = resolveAsFile(dir.resolve("index"))

    private fun resolveAsFile(file: File): File? {
        if (file.isFile) return file

        val js = File(file.path + ".js")
        if (js.isFile) return js

        return null
    }

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