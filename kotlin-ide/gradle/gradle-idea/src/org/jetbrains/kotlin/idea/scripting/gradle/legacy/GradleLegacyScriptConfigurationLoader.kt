/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.legacy

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.idea.scripting.gradle.isGradleKotlinScript
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

/**
 * Loader that performs loading for .gralde.kts scripts configuration through the [DefaultScriptingSupport]
 *
 * TODO(gradle6): remove
 */
class GradleLegacyScriptConfigurationLoader(project: Project) : DefaultScriptConfigurationLoader(project) {
    private val buildRootsManager = GradleBuildRootsManager.getInstance(project)

    override fun isPostponedLoad(virtualFile: VirtualFile): Boolean = isGradleKotlinScript(virtualFile)

    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition) = true

    override fun loadDependencies(
        isFirstLoad: Boolean,
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        val vFile = ktFile.originalFile.virtualFile

        if (!isGradleKotlinScript(vFile)) return false

        if (!buildRootsManager.isAffectedGradleProjectFile(vFile.path)) {
            // not known gradle file and not configured as standalone script
            // skip
            return true
        }

        // Gradle read files from FS
        GlobalScope.launch(EDT(project)) {
            runWriteAction {
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        }

        val result = getConfigurationThroughScriptingApi(ktFile, vFile, scriptDefinition)
        context.saveNewConfiguration(vFile, result)
        return true
    }

    override fun getInputsStamp(virtualFile: VirtualFile, file: KtFile): CachedConfigurationInputs {
        return getGradleScriptInputsStamp(project, virtualFile, file)
            ?: super.getInputsStamp(virtualFile, file)
    }
}
