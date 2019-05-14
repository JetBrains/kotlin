/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.NODE_MODULES
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import java.io.File

internal open class NpmProjectModules(
    val dir: File,
    val nodeModulesDir: File = dir.resolve(NODE_MODULES)
) {
    /**
     * Require [request] nodejs module and return canonical path to it's main js file.
     */
    fun require(request: String): String {
        return resolve(request)?.canonicalPath ?: error("Cannot find node module \"$request\" in \"$this\"")
    }

    open val parent: NpmProjectModules?
        get() = null

    /**
     * Find node module according to https://nodejs.org/api/modules.html#modules_all_together,
     * with exception that instead of traversing parent folders, we are traversing parent projects
     */
    internal fun resolve(name: String, context: File = dir): File? =
        if (name.startsWith("/")) resolve(name.removePrefix("/"), File("/"))
        else resolveAsRelative("./", name, context)
            ?: resolveAsRelative("/", name, context)
            ?: resolveAsRelative("../", name, context)
            ?: resolveInNodeModulesDir(name, nodeModulesDir)
            ?: parent?.resolve(name)

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
        val packageJsonFile = dir.resolve(PACKAGE_JSON)
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

    private fun resolveIndex(dir: File): File? = resolveAsFile(dir.resolve(INDEX_FILE_NAME))

    private fun resolveAsFile(file: File): File? {
        if (file.isFile) return file

        val js = File(file.path + JS_SUFFIX)
        if (js.isFile) return js

        return null
    }

    override fun toString(): String = "$dir"

    companion object {
        const val JS_SUFFIX = ".js"
        const val INDEX_FILE_NAME = "index"
    }
}