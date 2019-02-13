/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineableCoroutineBody
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata

// Goes through `suspend inline` function definitions and transforms into two entities:
// 1. A `suspend` function definition, which could be invoked at run time
// 2. A `suspend inline` function definition, which doesn't work at run time, but is understood by the inliner.
class InlineSuspendFunctionSplitter(
    val scope: ImportIntoFragmentInliningScope
) : JsVisitorWithContextImpl() {

    override fun visit(x: JsInvocation, ctx: JsContext<JsNode>): Boolean {
        // Is it `defineInlineFunction('tag', ...)`?
        InlineMetadata.decompose(x)?.let { metadata ->
            val fn = metadata.function
            if (fn.function.coroutineMetadata != null) {
                // This function will be exported to JS
                ctx.replaceMe(scope.importFunctionDefinition(InlineFunctionDefinition(fn, metadata.tag.value)))

                // Original function should be not be transformed into a state machine
                fn.function.name = null
                fn.function.coroutineMetadata = null
                fn.function.isInlineableCoroutineBody = true

                // Keep the `defineInlineFunction` for the inliner to find
                lastStatementLevelContext.addNext(x.makeStmt())

            }
            return false
        }

        // Is it `wrapFunction(...)`? Which means it'a a private inline function
        InlineMetadata.tryExtractFunction(x)?.let { fn ->
            if (fn.function.coroutineMetadata != null) {
                // This function will be exported to JS
                ctx.replaceMe(scope.importFunctionDefinition(InlineFunctionDefinition(fn, null)))
            }
            return false
        }

        return super.visit(x, ctx)
    }
}
