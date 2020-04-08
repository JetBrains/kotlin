/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testIntegration

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testIntegration.TestCreator
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinTestCreator : TestCreator {
    private fun getTarget(editor: Editor, file: PsiFile): KtNamedDeclaration? {
        return file.findElementAt(editor.caretModel.offset)?.parents
            ?.firstOrNull { it is KtClassOrObject || it is KtNamedDeclaration && it.parent is KtFile } as? KtNamedDeclaration
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val declaration = getTarget(editor, file) ?: return false
        return KotlinCreateTestIntention().applicabilityRange(declaration) != null
    }

    override fun createTest(project: Project, editor: Editor, file: PsiFile) {
        val declaration = getTarget(editor, file) ?: return
        KotlinCreateTestIntention().applyTo(declaration, editor)
    }
}