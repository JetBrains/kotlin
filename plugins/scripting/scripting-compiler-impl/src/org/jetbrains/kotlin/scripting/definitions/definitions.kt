/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.script.experimental.api.SourceCode

inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

@OptIn(ExperimentalContracts::class)
fun PsiFile.isScript(): Boolean {
    contract {
        returns(true) implies (this@isScript is KtFile)
    }

    // Do not use psiFile.script, see comments in findScriptDefinition
    if (this !is KtFile/* || this.script == null*/) return false

    // Sometimes - i.e. when event system is disabled for a view provider - requesting
    // virtual file directly from the viewProvider is the only way of obtaining it
    val virtualFile = virtualFile ?: originalFile.virtualFile ?: viewProvider.virtualFile
    if (virtualFile.isNonScript()) return false

    return true
}

fun PsiFile.findScriptDefinition(): ScriptDefinition? {
    return if (isScript()) findScriptDefinition(project, KtFileScriptSource(this))
    else null
}

@Deprecated("Use PsiFile.findScriptDefinition() instead")
fun VirtualFile.findScriptDefinition(project: Project): ScriptDefinition? {
    if (!isValid || isNonScript()) return null

    // Do not use psiFile.script here because this method can be called during indexes access
    // and accessing stubs may cause deadlock
    // TODO: measure performance effect and if necessary consider detecting indexing here or using separate logic for non-IDE operations to speed up filtering
    if (runReadAction { PsiManager.getInstance(project).findFile(this) as? KtFile }/*?.script*/ == null) return null

    return findScriptDefinition(project, VirtualFileScriptSource(this))
}

fun findScriptDefinition(project: Project, script: SourceCode): ScriptDefinition {
    val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) ?: return null
        ?: throw IllegalStateException("Unable to get script definition: ScriptDefinitionProvider is not configured.")

    return scriptDefinitionProvider.findDefinition(script) ?: scriptDefinitionProvider.getDefaultDefinition()
}

private const val JAVA_CLASS_FILE_TYPE_DOT_DEFAULT_EXTENSION = ".class"
fun VirtualFile.isNonScript(): Boolean = when (this) {
    is VirtualFileWindow -> {
        // This file is an embedded Kotlin code snippet.
        !this.isKotlinFileType()
    }
    else -> {
        if (isDirectory) {
            true
        } else {
            val nameSeq = nameSequence
            nameSeq.endsWith(KotlinFileType.DOT_DEFAULT_EXTENSION) ||
                    nameSeq.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION) ||
                    nameSeq.endsWith(JAVA_CLASS_FILE_TYPE_DOT_DEFAULT_EXTENSION) ||
                    !this.isKotlinFileType()
        }
    }
}

private fun VirtualFile.isKotlinFileType(): Boolean {
    if (nameSequence.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)) return true

    val typeRegistry = FileTypeRegistry.getInstance()
    return typeRegistry.getFileTypeByFile(this) == KotlinFileType.INSTANCE ||
            typeRegistry.getFileTypeByFileName(name) == KotlinFileType.INSTANCE
}
