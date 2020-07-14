/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isUnit

class KotlinInlineFunctionHandler : KotlinInlineActionHandler() {
    //TODO: overrides etc
    override fun canInlineElement(element: PsiElement) = element is KtNamedFunction && element.hasBody()

    override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        element as KtNamedFunction
        val nameReference = editor?.findSimpleNameReference()

        val recursive = element.isRecursive()
        if (recursive && nameReference == null) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                KotlinBundle.message("text.inline.recursive.function.is.supported.only.on.references")
            )

            CommonRefactoringUtil.showErrorHint(project, editor, message, KotlinBundle.message("title.inline.function"), null)
            return
        }

        val descriptor = element.unsafeResolveToDescriptor() as SimpleFunctionDescriptor
        val returnType = descriptor.returnType
        val codeToInline = buildCodeToInline(
            element,
            returnType,
            element.hasDeclaredReturnType() || (element.hasBlockBody() && returnType?.isUnit() == true),
            element.bodyExpression!!,
            element.hasBlockBody(),
            editor
        ) ?: return

        val replacementStrategy = CallableUsageReplacementStrategy(codeToInline, inlineSetter = false)

        val dialog = KotlinInlineFunctionDialog(
            project, element, nameReference, replacementStrategy,
            allowInlineThisOnly = recursive
        )
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            dialog.show()
        } else {
            dialog.doAction()
        }
    }

    private fun KtNamedFunction.isRecursive(): Boolean {
        val context = analyzeWithContent()
        return bodyExpression?.includesCallOf(context[BindingContext.FUNCTION, this] ?: return false, context) ?: false
    }

    private fun KtExpression.includesCallOf(descriptor: FunctionDescriptor, context: BindingContext): Boolean {
        val refDescriptor = getResolvedCall(context)?.resultingDescriptor
        return descriptor == refDescriptor || anyDescendantOfType<KtExpression> {
            it !== this && descriptor == it.getResolvedCall(context)?.resultingDescriptor
        }
    }

}