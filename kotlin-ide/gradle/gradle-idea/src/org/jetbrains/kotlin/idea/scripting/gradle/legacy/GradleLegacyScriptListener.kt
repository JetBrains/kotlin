/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.legacy

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.scripting.gradle.isGradleKotlinScript
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

// called from GradleScriptListener
// todo(gradle6): remove
class GradleLegacyScriptListener(project: Project) : ScriptChangeListener(project) {
    private val buildRootsManager = GradleBuildRootsManager.getInstance(project)

    override fun isApplicable(vFile: VirtualFile) =
        isGradleKotlinScript(vFile)

    override fun editorActivated(vFile: VirtualFile) =
        checkUpToDate(vFile)

    override fun documentChanged(vFile: VirtualFile) =
        checkUpToDate(vFile)

    private fun checkUpToDate(vFile: VirtualFile) {
        if (!buildRootsManager.isAffectedGradleProjectFile(vFile.path)) return

        val file = getAnalyzableKtFileForScript(vFile)
        if (file != null) {
            // *.gradle.kts file was changed
            default.suggestToUpdateConfigurationIfOutOfDate(file)
        }
    }
}
