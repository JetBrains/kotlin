/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

fun PsiFile.findScriptDefinition(): ScriptDefinition? {
    // Do not use psiFile.script, see comments in findScriptDefinition
    if (this !is KtFile/* || this.script == null*/) return null
    val file = virtualFile ?: originalFile.virtualFile ?: return null
    if (file.isNonScript()) return null

    return findScriptDefinitionByFileName(project, file.name)
}

fun VirtualFile.findScriptDefinition(project: Project): ScriptDefinition? {
    if (!isValid || isNonScript()) return null
    // Do not use psiFile.script here because this method can be called during indexes access
    // and accessing stubs may cause deadlock
    // TODO: measure performance effect and if necessary consider detecting indexing here or using separate logic for non-IDE operations to speed up filtering

    if (runReadAction { PsiManager.getInstance(project).findFile(this) as? KtFile }/*?.script*/ == null) return null

    return findScriptDefinitionByFileName(project, name)
}

fun findScriptDefinitionByFileName(project: Project, fileName: String): ScriptDefinition {
    val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) ?: return null
        ?: throw IllegalStateException("Unable to get script definition: ScriptDefinitionProvider is not configured.")

    return scriptDefinitionProvider.findDefinition(fileName) ?: scriptDefinitionProvider.getDefaultDefinition()
}

private fun VirtualFile.isNonScript(): Boolean =
    isDirectory ||
            extension == KotlinFileType.EXTENSION ||
            extension == JavaClassFileType.INSTANCE.defaultExtension ||
            !this.isKotlinFileType()

private fun VirtualFile.isKotlinFileType(): Boolean {
    val typeRegistry = FileTypeRegistry.getInstance()
    return typeRegistry.getFileTypeByFile(this) == KotlinFileType.INSTANCE ||
            typeRegistry.getFileTypeByFileName(name) == KotlinFileType.INSTANCE
}
