/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.Serializable
import kotlin.script.experimental.api.ScriptDiagnostic

/**
 * Cached configurations for the file's specific snapshot state.
 *
 * The writer should put related inputs snapshot for loaded configuration.
 * This would allow making up-to-date checks for existed entry.
 *
 * The configuration may be loaded but not applied. So, it makes
 * sense to do up-to-date check on loaded configuration (not on applied).
 * For those reasons, we are storing both for each file.
 */
interface ScriptConfigurationCache {
    operator fun get(file: VirtualFile): ScriptConfigurationState?

    fun setApplied(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot)
    fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot)
    fun markOutOfDate(scope: ScriptConfigurationCacheScope)

    fun allApplied(): Map<VirtualFile, ScriptCompilationConfigurationWrapper>
    fun clear()
}

sealed class ScriptConfigurationCacheScope {
    object All : ScriptConfigurationCacheScope()
    class File(val file: KtFile) : ScriptConfigurationCacheScope()
    class Except(val file: KtFile) : ScriptConfigurationCacheScope()
}

data class ScriptConfigurationState(
    val applied: ScriptConfigurationSnapshot? = null,
    val loaded: ScriptConfigurationSnapshot? = null
) {
    fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile? = null): Boolean =
        (loaded ?: applied)?.inputs?.isUpToDate(project, file, ktFile) ?: false
}

data class ScriptConfigurationSnapshot(
    val inputs: CachedConfigurationInputs,
    val reports: List<ScriptDiagnostic>,
    val configuration: ScriptCompilationConfigurationWrapper?
)

interface CachedConfigurationInputs: Serializable {
    fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile? = null): Boolean

    object OutOfDate : CachedConfigurationInputs {
        override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean = false
    }

    data class PsiModificationStamp(
        val fileModificationStamp: Long,
        val psiModificationStamp: Long
    ) : CachedConfigurationInputs {
        override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean =
            get(project, file, ktFile) == this

        companion object {
            fun get(project: Project, file: VirtualFile, ktFile: KtFile?): PsiModificationStamp {
                val actualKtFile = project.getKtFile(file, ktFile)
                return PsiModificationStamp(
                    file.modificationStamp,
                    actualKtFile?.modificationStamp ?: 0
                )
            }
        }
    }

    data class SourceContentsStamp(val source: String) : CachedConfigurationInputs {
        override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean =
            get(project, file, ktFile) == this

        companion object {
            fun get(project: Project, file: VirtualFile, ktFile: KtFile?): SourceContentsStamp {
                val text = runReadAction {
                    FileDocumentManager.getInstance().getDocument(file)!!.text
                }

                return SourceContentsStamp(text)
            }
        }
    }
}