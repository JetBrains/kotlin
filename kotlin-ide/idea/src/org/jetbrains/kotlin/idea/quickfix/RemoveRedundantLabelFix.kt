/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveRedundantLabelFix(element: KtLabeledExpression) : KotlinQuickFixAction<KtLabeledExpression>(element) {
    override fun getText(): String = KotlinBundle.message("remove.redundant.label")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val labeledExpression = element ?: return
        val baseExpression = labeledExpression.baseExpression ?: return
        labeledExpression.replace(baseExpression)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtLabeledExpression>? {
            val labelReference = diagnostic.psiElement as? KtLabelReferenceExpression ?: return null
            val labeledExpression = labelReference.getStrictParentOfType<KtLabeledExpression>() ?: return null
            return RemoveRedundantLabelFix(labeledExpression)
        }
    }
}