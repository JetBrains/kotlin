/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptAdditionalIdeaDependenciesProvider
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleScriptAdditionalIdeaDependenciesProvider : ScriptAdditionalIdeaDependenciesProvider() {
    override fun getRelatedModules(file: VirtualFile, project: Project): List<Module> {
        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val projectSettings = gradleSettings.getLinkedProjectsSettings().filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: return emptyList()
        val includedModulesPath: List<String> = projectSettings.compositeBuild?.compositeParticipants?.mapNotNull { part ->
            projectSettings.modules.find { it == part.rootPath }
        } ?: emptyList()
        val includedModulesBuildSrcPaths = includedModulesPath.map { "$it/buildSrc" }

        val rootBuildSrcPath = "${projectSettings.externalProjectPath}/buildSrc"

        return (includedModulesPath + includedModulesBuildSrcPaths + rootBuildSrcPath).flatMap { path ->
            ModuleManager.getInstance(project).modules.filter {
                ExternalSystemApiUtil.getExternalProjectPath(it) == path
            }
        }
    }

    override fun getRelatedLibraries(file: VirtualFile, project: Project): List<Library> {
        return emptyList()
    }
}