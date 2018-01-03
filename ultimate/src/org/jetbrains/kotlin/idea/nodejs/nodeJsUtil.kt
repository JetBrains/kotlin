/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.framework.isGradleModule
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*

const val NODE_PATH_VAR = "NODE_PATH"

fun Module.getNodeJsEnvironmentVars(): EnvironmentVariablesData {
    val nodeJsClasspath = getNodeJsClasspath(this).joinToString(File.pathSeparator) {
        val basePath = project.basePath ?: return@joinToString it
        FileUtil.getRelativePath(basePath, it, '/') ?: it
    }
    return EnvironmentVariablesData.create(mapOf(NODE_PATH_VAR to nodeJsClasspath), true)
}

private fun addSingleModulePaths(target: Module, result: MutableList<String>) {
    val compilerExtension = CompilerModuleExtension.getInstance(target) ?: return
    result.addIfNotNull(compilerExtension.compilerOutputPath?.path)
    result.addIfNotNull(compilerExtension.compilerOutputPathForTests?.let { "${it.path}/lib" })
}

private fun ExternalProject.findProjectById(id: String): ExternalProject? {
    if (this.id == id) return this
    for (childProject in childProjects.values) {
        childProject.findProjectById(id)?.let { return it }
    }
    return null
}

private fun getNodeJsClasspath(module: Module): List<String> {
    if (module.isGradleModule()) {
        val gradleProjectPath = ExternalSystemModulePropertyManager.getInstance(module).getRootProjectPath()
        val projectCache = ExternalProjectDataCache.getInstance(module.project)
        val rootProject = projectCache.getRootExternalProject(GradleConstants.SYSTEM_ID, File(gradleProjectPath)) ?: return listOf()
        val externalProjectId = ExternalSystemApiUtil.getExternalProjectId(module) ?: return listOf()
        val buildDir = rootProject.findProjectById(externalProjectId)?.buildDir ?: return listOf()
        return listOfNotNull(PathUtil.toSystemIndependentName(buildDir.absolutePath) + "/nodejs_modules")
    }

    val result = ArrayList<String>()
    ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule {
        addSingleModulePaths(it, result)
        true
    }
    return result
}