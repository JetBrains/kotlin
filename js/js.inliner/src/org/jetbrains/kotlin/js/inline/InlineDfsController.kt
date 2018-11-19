/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsName
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

class InlineDfsController(
    val trace: DiagnosticSink,
    val functions: Map<JsName, FunctionWithWrapper>,
    val accessors: Map<String, FunctionWithWrapper>,
    val functionContext: FunctionContext
) {

    val functionsByWrapperNodes =
        HashMap<JsBlock, FunctionWithWrapper>()
    val functionsByFunctionNodes =
        HashMap<JsFunction, FunctionWithWrapper>()

    init {
        (functions.values.asSequence() + accessors.values.asSequence()).forEach { f ->
            functionsByFunctionNodes[f.function] = f
            if (f.wrapperBody != null) {
                functionsByWrapperNodes[f.wrapperBody] = f
            }
        }
    }

    private val processedFunctions = IdentitySet<JsFunction>()
    private val inProcessFunctions = IdentitySet<JsFunction>()
    // these are needed for error reporting, when inliner detects cycle
    private val namedFunctionsStack = Stack<JsFunction>()

    private val inlineCallInfos = LinkedList<JsCallInfo>()

    val currentNamedFunction: JsFunction?
        get() = if (namedFunctionsStack.empty()) null else namedFunctionsStack.peek()


    fun startFunction(function: JsFunction) {
        assert(!inProcessFunctions.contains(function)) { "Inliner has revisited function" }
        inProcessFunctions.add(function)

        if (function in functionsByFunctionNodes.keys) {
            namedFunctionsStack.push(function)
        }
    }

    fun endFunction(function: JsFunction) {
        refreshLabelNames(function.body, function.scope)

        removeUnusedLocalFunctionDeclarations(function)
        processedFunctions.add(function)

        FunctionPostProcessor(function).apply()

        assert(inProcessFunctions.contains(function))
        inProcessFunctions.remove(function)

        if (!namedFunctionsStack.empty() && namedFunctionsStack.peek() == function) {
            namedFunctionsStack.pop()
        }
    }


    // Return true iff the definition should be visited by the inliner
    fun visitCall(call: JsInvocation): Boolean {
        val definition = functionContext.getFunctionDefinition(call)

        currentNamedFunction?.let {
            inlineCallInfos.add(JsCallInfo(call, it))
        }


        if (inProcessFunctions.contains(definition.function)) {
            reportInlineCycle(call, definition.function)
        } else if (!processedFunctions.contains(definition.function)) {
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