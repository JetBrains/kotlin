/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.dependencies.ScriptDependencies

interface ScriptDependenciesProvider {

    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getScriptDependencies(file: VirtualFile): ScriptDependencies? =
        getScriptConfigurationResult(file)?.valueOrNull()?.legacyDependencies

    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getScriptDependencies(file: PsiFile): ScriptDependencies? =
        getScriptConfigurationResult(file.virtualFile ?: file.originalFile.virtualFile)?.valueOrNull()?.legacyDependencies

    fun getScriptConfigurationResult(file: VirtualFile): ScriptCompilationConfigurationResult? = null

    fun getScriptConfigurationResult(file: PsiFile): ScriptCompilationConfigurationResult? =
        getScriptConfigurationResult(file.virtualFile ?: file.originalFile.virtualFile)

    companion object {
        fun getInstance(project: Project): ScriptDependenciesProvider? =
            ServiceManager.getService(project, ScriptDependenciesProvider::class.java)
    }
}
