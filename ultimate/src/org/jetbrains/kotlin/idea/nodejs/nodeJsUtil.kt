/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.js.shouldUseJpsOutput
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.util.*

const val NODE_PATH_VAR = "NODE_PATH"

fun Module.getNodeJsEnvironmentVars(isForTests: Boolean): EnvironmentVariablesData {
    val nodeJsClasspath = getNodeJsClasspath(this, isForTests).joinToString(File.pathSeparator) {
        val basePath = project.basePath ?: return@joinToString it
        FileUtil.getRelativePath(basePath, it, '/') ?: it
    }
    return EnvironmentVariablesData.create(mapOf(NODE_PATH_VAR to nodeJsClasspath), true)
}

private fun addSingleModulePaths(
    target: Module,
    isForTests: Boolean,
    result: MutableList<String>
) {
    val compilerExtension = CompilerModuleExtension.getInstance(target) ?: return
    if (isForTests) {
        result.addIfNotNull(compilerExtension.compilerOutputPath?.path)
        result.addIfNotNull(compilerExtension.compilerOutputPathForTests?.let { "${it.path}/lib" })
    } else {
        result.addIfNotNull(compilerExtension.compilerOutputPath?.let { "${it.path}/lib" })
    }
}

private fun getNodeJsClasspath(module: Module, isForTests: Boolean): List<String> {
    if (!module.shouldUseJpsOutput) return emptyList()

    val result = ArrayList<String>()
    ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule {
        addSingleModulePaths(it, isForTests, result)
        true
    }
    return result
}