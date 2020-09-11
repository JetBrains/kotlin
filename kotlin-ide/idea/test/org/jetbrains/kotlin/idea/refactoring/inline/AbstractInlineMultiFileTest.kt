/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.google.gson.JsonObject
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.refactoring.AbstractMultifileRefactoringTest
import org.jetbrains.kotlin.idea.refactoring.runRefactoringTest

abstract class AbstractInlineMultiFileTest : AbstractMultifileRefactoringTest() {
    override fun runRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project) {
        runRefactoringTest(path, config, rootDir, project, InlineAction)
    }
}

object InlineAction: AbstractMultifileRefactoringTest.RefactoringAction {
    override fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject) {
        val targetElement = elementsAtCaret.single()
        InlineActionHandler.EP_NAME.extensions.firstOrNull { it.canInlineElement(targetElement) }?.inlineElement(
            targetElement.project,
            targetElement.findExistingEditor(),
            targetElement,
        )
    }
}
