/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import org.jetbrains.kotlin.idea.scratch.getScratchFile
import org.jetbrains.kotlin.idea.scratch.getScratchFileFromSelectedEditor
import javax.swing.Icon

abstract class ScratchAction(message: String, icon: Icon) : AnAction(message, message, icon) {
    override fun update(e: AnActionEvent) {
        val scratchFile = e.getData(CommonDataKeys.EDITOR)
            ?.let { TextEditorProvider.getInstance().getTextEditor(it).getScratchFile() }
            ?: e.project?.let { getScratchFileFromSelectedEditor(it) }

        e.presentation.isVisible = scratchFile != null
    }
}