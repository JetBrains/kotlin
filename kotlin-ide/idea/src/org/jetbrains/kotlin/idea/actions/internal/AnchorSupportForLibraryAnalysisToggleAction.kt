/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.project.KotlinAnchorSupportForLibraryAnalysisComponent
import org.jetbrains.kotlin.idea.project.useAnchorServices

class AnchorSupportForLibraryAnalysisToggleAction : ToggleAction(
    KotlinBundle.message("toggle.anchor.support.for.library.analysis"),
    KotlinBundle.message("enable.components.for.library.to.source.analysis.in.kotlin"),
    null
) {
    override fun isSelected(e: AnActionEvent): Boolean =
        e.project?.useAnchorServices == true

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.let {
            KotlinAnchorSupportForLibraryAnalysisComponent.setState(it, state)
        }
    }

}