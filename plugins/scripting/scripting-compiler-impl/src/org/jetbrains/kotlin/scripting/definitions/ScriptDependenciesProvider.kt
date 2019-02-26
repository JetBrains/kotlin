/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import kotlin.script.experimental.dependencies.ScriptDependencies

interface ScriptDependenciesProvider {
    fun getScriptDependencies(file: VirtualFile): ScriptDependencies?
    fun getScriptDependencies(file: PsiFile) = getScriptDependencies(file.virtualFile ?: file.originalFile.virtualFile)

    companion object {
        fun getInstance(project: Project): ScriptDependenciesProvider? =
            ServiceManager.getService(project, ScriptDependenciesProvider::class.java)
    }
}
