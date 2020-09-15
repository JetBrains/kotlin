/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.surroundWith

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.surroundWith.KotlinExpressionSurrounder
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerEvaluationBundle
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinRuntimeTypeEvaluator
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class KotlinRuntimeTypeCastSurrounder : KotlinExpressionSurrounder() {

    override fun isApplicable(expression: KtExpression): Boolean {
        if (!super.isApplicable(expression)) return false

        if (!expression.isPhysical) return false
        val file = expression.containingFile
        if (file !is KtCodeFragment) return false

        val type = expression.analyze(BodyResolveMode.PARTIAL).getType(expression) ?: return false

        return TypeUtils.canHaveSubtypes(KotlinTypeChecker.DEFAULT, type)
    }

    override fun surroundExpression(project: Project, editor: Editor, expression: KtExpression): TextRange? {
        val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context
        val debuggerSession = debuggerContext.debuggerSession
        if (debuggerSession != null) {
            val progressWindow = ProgressWindow(true, expression.project)
            val worker = SurroundWithCastWorker(editor, expression, debuggerContext, progressWindow)
            progressWindow.title = JavaDebuggerBundle.message("title.evaluating")
            debuggerContext.debugProcess?.managerThread?.startProgress(worker, progressWindow)
        }
        return null
    }

    override fun getTemplateDescription(): String {
        return KotlinDebuggerEvaluationBundle.message("surround.with.runtime.type.cast.template")
    }

    private inner class SurroundWithCastWorker(
        private val myEditor: Editor,
        expression: KtExpression,
        context: DebuggerContextImpl,
        indicator: ProgressIndicator
    ) : KotlinRuntimeTypeEvaluator(myEditor, expression, context, indicator) {

        override fun typeCalculationFinished(type: KotlinType?) {
            if (type == null) return

            hold()

            val project = myEditor.project
            DebuggerInvocationUtil.invokeLater(project, Runnable {
                object : WriteCommandAction<Any>(project, JavaDebuggerBundle.message("command.name.surround.with.runtime.cast")) {
                    override fun run(result: Result<Any>) {
                        try {
                            val factory = KtPsiFactory(myElement.project)

                            val fqName = DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!)
                            val parentCast = factory.createExpression("(expr as " + fqName.asString() + ")") as KtParenthesizedExpression
                            val cast = parentCast.expression as KtBinaryExpressionWithTypeRHS
                            cast.left.replace(myElement)
                            val expr = myElement.replace(parentCast) as KtExpression

                            ShortenReferences.DEFAULT.process(expr)

                            val range = expr.textRange
                            myEditor.selectionModel.setSelection(range.startOffset, range.endOffset)
                            myEditor.caretModel.moveToOffset(range.endOffset)
                            myEditor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
                        } finally {
                            release()
                        }
                    }
                }.execute()
            }, myProgressIndicator.modalityState)
        }

    }
}
