/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.psiElement
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.clean.removeUnusedLocalFunctionDeclarations
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.util.FunctionWithWrapper
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.refreshLabelNames
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import java.util.*

/**
 *  There are two ways the inliner may arrive to an inline function declaration. Either the topmost visitor arrive to a declaration, which
 *  has never been used before. Or it visits it's invocation, and has to obtain the inline function body. Current strategy is to processes
 *  the inline function declaration before inlining it's invocation.
 *
 *  Thus the inliner effectively implements a DFS. InlinerCycleReporter manages the DFS state. Also it detects and reports cycles.
 */
class InlinerCycleReporter(
    val trace: DiagnosticSink,
    private val functionContext: FunctionContext
) {

    private val processedFunctions = IdentitySet<JsFunction>()
    private val inProcessFunctions = IdentitySet<JsFunction>()
    // these are needed for error reporting, when inliner detects cycle
    private val namedFunctionsStack = Stack<JsFunction>()

    private val inlineCallInfos = LinkedList<JsCallInfo>()

    // TODO This looks like a hack
    val currentNamedFunction: JsFunction?
        get() = if (namedFunctionsStack.empty()) null else namedFunctionsStack.peek()


    fun startFunction(function: JsFunction) {
        assert(!inProcessFunctions.contains(function)) { "Inliner has revisited function" }
        inProcessFunctions.add(function)

        if (function in functionContext.functionsByFunctionNodes.keys) {
            namedFunctionsStack.push(function)
        }
    }

    fun endFunction(function: JsFunction) {
        processedFunctions.add(function)

        assert(inProcessFunctions.contains(function))
        inProcessFunctions.remove(function)

        if (!namedFunctionsStack.empty() && namedFunctionsStack.peek() == function) {
            namedFunctionsStack.pop()
        }
    }


    // Return true iff the definition should be visited by the inliner
    fun shouldProcess(definition: FunctionWithWrapper, call: JsInvocation): Boolean {

        currentNamedFunction?.let {
            inlineCallInfos.add(JsCallInfo(call, it))
        }

        if (definition.function in inProcessFunctions) {
            reportInlineCycle(call, definition.function)
        } else if (definition.function !in processedFunctions) {
            return true
        }

        return false
    }

    fun endVisit(x: JsInvocation) {
        if (!inlineCallInfos.isEmpty()) {
            if (inlineCallInfos.last.call == x) {
                inlineCallInfos.removeLast()
            }
        }
    }

    private fun reportInlineCycle(call: JsInvocation, calledFunction: JsFunction) {
        call.inlineStrategy = InlineStrategy.NOT_INLINE
        val it = inlineCallInfos.descendingIterator()

        while (it.hasNext()) {
            val callInfo = it.next()
            val psiElement = callInfo.call.psiElement

            val descriptor = callInfo.call.descriptor
            if (psiElement != null && descriptor != null) {
                trace.report(Errors.INLINE_CALL_CYCLE.on(psiElement, descriptor))
            }

            if (callInfo.containingFunction == calledFunction) {
                break
            }
        }
    }
}

private class JsCallInfo(val call: JsInvocation, val containingFunction: JsFunction)