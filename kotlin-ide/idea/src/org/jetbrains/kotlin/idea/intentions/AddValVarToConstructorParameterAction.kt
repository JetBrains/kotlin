/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.ValVarExpression
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.idea.util.mustHaveValOrVar
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

interface AddValVarToConstructorParameterAction {
    fun canInvoke(element: KtParameter): Boolean =
        element.valOrVarKeyword == null && ((element.parent as? KtParameterList)?.parent as? KtPrimaryConstructor)?.takeIf { it.mustHaveValOrVar() || !it.isExpectDeclaration() } != null

    operator fun invoke(element: KtParameter, editor: Editor?) {
        val project = element.project

        element.addBefore(KtPsiFactory(project).createValKeyword(), element.nameIdentifier)

        if (element.containingClass()?.isInline() == true || editor == null) return

        val parameter = element.createSmartPointer().let {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            it.element
        } ?: return

        editor.caretModel.moveToOffset(parameter.startOffset)

        TemplateBuilderImpl(parameter)
            .apply { replaceElement(parameter.valOrVarKeyword ?: return@apply, ValVarExpression) }
            .buildInlineTemplate()
            .let { TemplateManager.getInstance(project).startTemplate(editor, it) }
    }

    class Intention : SelfTargetingRangeIntention<KtParameter>(
        KtParameter::class.java,
        KotlinBundle.lazyMessage("add.val.var.to.primary.constructor.parameter")
    ), AddValVarToConstructorParameterAction {
        override fun applicabilityRange(element: KtParameter): TextRange? {
            if (!canInvoke(element)) return null
            val containingClass = element.getStrictParentOfType<KtClass>()
            if (containingClass?.isData() == true || containingClass?.isInline() == true) return null
            setTextGetter(KotlinBundle.lazyMessage("add.val.var.to.parameter.0", element.name ?: ""))
            return element.nameIdentifier?.textRange
        }

        override fun applyTo(element: KtParameter, editor: Editor?) = invoke(element, editor)
    }

    class QuickFix(parameter: KtParameter) :
        KotlinQuickFixAction<KtParameter>(parameter),
        AddValVarToConstructorParameterAction {
        override fun getText(): String {
            val element = this.element ?: return ""

            val key = when {
                element.getStrictParentOfType<KtClass>()?.isInline() == true -> "add.val.to.parameter.0"
                else -> "add.val.var.to.parameter.0"
            }

            return KotlinBundle.message(key, element.name ?: "")
        }

        override fun getFamilyName() = KotlinBundle.message("add.val.var.to.primary.constructor.parameter")

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            invoke(element ?: return, editor)
        }
    }

    object QuickFixFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = QuickFix(Errors.DATA_CLASS_NOT_PROPERTY_PARAMETER.cast(diagnostic).psiElement)
    }
}