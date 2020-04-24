/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.legacy

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.scripting.gradle.*

class GradleLegacyScriptListener(project: Project) : ScriptChangeListener(project) {
    override fun isApplicable(vFile: VirtualFile): Boolean {
        return isGradleKotlinScript(vFile)
    }

    override fun editorActivated(vFile: VirtualFile) {
        if (!isInAffectedGradleProjectFiles(project, vFile.path)) return

        if (useScriptConfigurationFromImportOnly()) {
            // do nothing
        } else {
            val file = getAnalyzableKtFileForScript(vFile) ?: return
            default.suggestToUpdateConfigurationIfOutOfDate(file)
        }
    }

    override fun documentChanged(vFile: VirtualFile) {
        if (!isInAffectedGradleProjectFiles(project, vFile.path)) return

        val file = getAnalyzableKtFileForScript(vFile)
        if (file != null) {
            // *.gradle.kts file was changed
            default.suggestToUpdateConfigurationIfOutOfDate(file)
        }
    }
}