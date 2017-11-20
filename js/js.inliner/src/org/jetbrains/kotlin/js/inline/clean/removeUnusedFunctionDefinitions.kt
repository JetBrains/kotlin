/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.isLocal
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.collectFunctionReferencesInside

/**
 * Removes unused function definitions:
 *  f: function() { return 10 }
 *
 * At now, it only removes unused local functions and function literals,
 * because named functions can be referenced from another module.
 */
fun removeUnusedFunctionDefinitions(root: JsNode, functions: Map<JsName, JsFunction>) {
    val removable = with(UnusedLocalFunctionsCollector(functions)) {
        process()
        accept(root)
        removableFunctions
    }.toSet()

    val remover = NodeRemover(JsStatement::class.java) { statement ->
        val expression = when (statement) {
            is JsExpressionStatement -> statement.expression
            is JsVars -> if (statement.vars.size == 1) statement.vars[0].initExpression else null
            else -> null
        }
        expression is JsFunction && expression in removable
    }

    remover.accept(root)
}

private class UnusedLocalFunctionsCollector(private val functions: Map<JsName, JsFunction>) : JsVisitorWithContextImpl() {
    private val tracker = ReferenceTracker<JsName, JsFunction>()
    private val processed = IdentitySet<JsFunction>()

    val removableFunctions: List<JsFunction>
        get() = tracker.removable

    fun process() {
        functions.filter { it.value.isLocal }
                 .forEach { tracker.addCandidateForRemoval(it.key, it.value) }

        for ((name, function) in functions) {
            if (function.isLocal) {
                processLocalFunction(name, function)
            } else {
                processNonLocalFunction(function)
            }

            processed.add(function)
        }
    }

    override fun visit(x: JsPropertyInitializer, ctx: JsContext<*>): Boolean {
        val value = x.valueExpr

        return when (value) {
            is JsFunction -> !wasProcessed(value)
            else -> super.visit(x, ctx)
        }
    }

    override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean = !wasProcessed(x)

    override fun endVisit(x: JsFunction, ctx: JsContext<*>) {
        processed.add(x)
    }

    override fun endVisit(x: JsNameRef, ctx: JsContext<*>) {
        val name = x.name
        if (isFunctionReference(x) && name != null) {
            tracker.markReachable(name)
        }
    }

    private fun processLocalFunction(name: JsName, function: JsFunction) {
        for (referenced in collectFunctionReferencesInside(function)) {
            tracker.addRemovableReference(name, referenced)
        }
    }

    private fun processNonLocalFunction(function: JsFunction) {
        for (referenced in collectFunctionReferencesInside(function)) {
            tracker.markReachable(referenced)
        }
    }

    private fun isFunctionReference(nameRef: HasName?): Boolean {
        return nameRef?.name?.staticRef is JsFunction
    }

    private fun wasProcessed(function: JsFunction?): Boolean = function != null && function in processed
}

