/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineableCoroutineBody
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata

class InlineSuspendFunctionSplitter(
    val scope: ProgramFragmentInliningScope
) : JsVisitorWithContextImpl() {

    override fun endVisit(x: JsExpressionStatement, ctx: JsContext<*>) {
        val e = x.expression
        if (e is JsBinaryOperation) {
            if (e.operator == JsBinaryOperator.ASG) {
                e.arg2?.let { argument2 ->
                    val splitSuspendInlineFunction = splitExportedSuspendInlineFunctionDeclarations(argument2)
                    if (splitSuspendInlineFunction != null) {
                        e.arg2 = splitSuspendInlineFunction
                    }
                }
            }
        }

        super.endVisit(x, ctx)
    }

    override fun endVisit(x: JsVars.JsVar, ctx: JsContext<*>) {
        val initExpression = x.initExpression ?: return

        val splitSuspendInlineFunction = splitExportedSuspendInlineFunctionDeclarations(initExpression)
        if (splitSuspendInlineFunction != null) {
            x.initExpression = splitSuspendInlineFunction
        }
    }

    private fun splitExportedSuspendInlineFunctionDeclarations(expression: JsExpression): JsFunction? {
        val inlineMetadata = InlineMetadata.decompose(expression)
        if (inlineMetadata != null) {
            inlineMetadata.function.let { f ->
                if (f.function.coroutineMetadata != null) {
                    val statementContext = lastStatementLevelContext

                    // This function will be exported to JS
                    val function = scope.importFunctionDefinition(InlineFunctionDefinition(inlineMetadata.function, inlineMetadata.tag.value))

                    // Original function should be not be transformed into a state machine
                    f.function.name = null
                    f.function.coroutineMetadata = null
                    f.function.isInlineableCoroutineBody = true

                    // Keep the `defineInlineFunction` for the inliner to find
                    statementContext.addNext(expression.makeStmt())

                    // Return the function body to be used without inlining.
                    return function
                }
            }
        }
        return null
    }
}
