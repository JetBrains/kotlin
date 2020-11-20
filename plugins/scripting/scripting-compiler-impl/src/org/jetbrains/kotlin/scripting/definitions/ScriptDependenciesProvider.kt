/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.dependencies.ScriptDependencies

open class ScriptDependenciesProvider constructor(
    protected val project: Project
) {
    @Suppress("DEPRECATION")
    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getScriptDependencies(file: VirtualFile): ScriptDependencies? {
        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null
        return getScriptConfiguration(ktFile)?.legacyDependencies
    }

    @Suppress("DEPRECATION")
    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getScriptDependencies(file: PsiFile): ScriptDependencies? {
        if (file !is KtFile) return null
        return getScriptConfiguration(file)?.legacyDependencies
    }

    open fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? = null

    open fun getScriptConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? = getScriptConfigurationResult(file)?.valueOrNull()

    companion object {
        fun getInstance(project: Project): ScriptDependenciesProvider? =
            ServiceManager.getService(project, ScriptDependenciesProvider::class.java)
    }
}
