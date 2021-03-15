/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea.quickfixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty.KotlinIntroducePropertyHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHintByKey
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParentContainedIn
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SerializationErrors
import org.jetbrains.kotlinx.serialization.idea.KotlinSerializationBundle

internal class JsonRedundantQuickFix(expression: KtCallExpression) : KotlinQuickFixAction<KtCallExpression>(expression) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        editor ?: return
        val element = element ?: return

        selectContainer(element, project, editor) {
            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)

            val outermostParent = element.getOutermostParentContainedIn(it)
            if (outermostParent == null) {
                showErrorHintByKey(project, editor, "cannot.refactor.no.container", text)
                return@selectContainer
            }
            KotlinIntroducePropertyHandler().doInvoke(project, editor, file, listOf(element), outermostParent)
        }
    }

    override fun getFamilyName(): String = text

    override fun getText(): String = KotlinSerializationBundle.message("extract.json.to.property")

    object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.factory != SerializationErrors.JSON_FORMAT_REDUNDANT) return null
            val castedDiagnostic = SerializationErrors.JSON_FORMAT_REDUNDANT.cast(diagnostic)

            val element: KtCallExpression = castedDiagnostic.psiElement as? KtCallExpression ?: return null
            return JsonRedundantQuickFix(element)
        }
    }

    private fun selectContainer(element: PsiElement, project: Project, editor: Editor, onSelect: (PsiElement) -> Unit) {
        val parent = element.parent ?: throw AssertionError("Should have at least one parent")

        val containers = parent.getExtractionContainers(strict = true, includeAll = true)
            .filter { it is KtClassBody || (it is KtFile && !it.isScript()) }

        if (containers.isEmpty()) {
            showErrorHintByKey(project, editor, "cannot.refactor.no.container", text)
            return
        }

        chooseContainerElementIfNecessary(
            containers,
            editor,
            KotlinBundle.message("title.select.target.code.block"),
            true,
            { it },
            { onSelect(it) }
        )
    }

}
