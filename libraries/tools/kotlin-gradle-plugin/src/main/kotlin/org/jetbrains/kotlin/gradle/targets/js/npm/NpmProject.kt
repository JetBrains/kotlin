/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.JsonObject
import groovy.transform.TailRecursive
import org.gradle.api.Project
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
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
        exec.args = listOf(getModuleEntryPath(tool)) + args
    }

    val hoistGradleNodeModules: Boolean
        get() = project.nodeJs.root.packageManager.shouldHoistGradleNodeModules(project)

    val gradleNodeModulesDir: File
        get() =
            if (hoistGradleNodeModules) project.rootProject.npmProject.nodeModulesDir
            else nodeModulesDir

    open fun moduleOutput(compilationTask: Kotlin2JsCompile): File {
        return compileOutputCopyDest?.resolve(compilationTask.outputFile.name) ?: compilationTask.outputFile
    }

    fun getModuleEntryPath(name: String): String {
        return findModuleEntry(name)?.canonicalPath ?: error("Cannot find node module $name in $this")
    }

    @TailRecursive
    fun findModuleEntry(name: String): File? {
        val file = nodeModulesDir.resolve(name)
        if (file.isFile) return file
        if (file.isDirectory) {
            val packageJsonFile = file.resolve("package.json")
            val main: String = (if (packageJsonFile.isFile) {
                val packageJson = packageJsonFile.reader().use {
                    Gson().fromJson(it, JsonObject::class.java)
                }

                packageJson["main"] as? String?
                    ?: packageJson["module"] as? String?
                    ?: packageJson["browser"] as? String?
            } else null) ?: "index.js"

            val mainFile = file.resolve(main)
            if (mainFile.isFile) return mainFile
        }

        val parent = project.parent
        return if (!searchInParents || parent == null) null
        else NpmProject[parent].findModuleEntry(name)
    }

    open fun configureCompilation(compilation: KotlinJsCompilation) = Unit

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