/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline.clean

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.staticRef
import com.google.dart.compiler.backend.js.ast.metadata.isLocal

import com.intellij.util.containers.Stack
import java.util.IdentityHashMap
import org.jetbrains.k2js.inline.util.IdentitySet
import org.jetbrains.k2js.inline.util.collectReferencesInside
import org.jetbrains.k2js.inline.util.collectFunctionReferencesInside

/**
 * Removes unused function definitions:
 *  f: function() { return 10 }
 *
 * At now, it only removes unused local functions and function literals,
 * because named functions can be referenced from another module.
 */
public fun removeUnusedFunctionDefinitions(root: JsNode, functions: Map<JsName, JsFunction>) {
    val removable = with(UnusedLocalFunctionsCollector(functions)) {
        process()
        accept(root)
        removableFunctions
    }

    NodeRemover(javaClass<JsPropertyInitializer>()) {
        val function = it.getValueExpr() as? JsFunction
        function in removable
    }.accept(root)
}

private class UnusedLocalFunctionsCollector(functions: Map<JsName, JsFunction>) : JsVisitorWithContextImpl() {
    private val tracker = ReferenceTracker<JsName, JsFunction>()
    private val functions = functions
    private val processed = IdentitySet<JsFunction>()

    public val removableFunctions: List<JsFunction>
        get() = tracker.removable

    public fun process() {
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

    override fun visit(x: JsPropertyInitializer?, ctx: JsContext?): Boolean {
        val value = x?.getValueExpr()

        return when (value) {
            is JsFunction -> !wasProcessed(value)
            else -> super.visit(x, ctx)
        }
    }

    override fun visit(x: JsFunction?, ctx: JsContext?): Boolean {
        return !(wasProcessed(x))
    }

    override fun endVisit(x: JsFunction?, ctx: JsContext?) {
        if (x == null) return

        processed.add(x)
    }

    override fun endVisit(x: JsNameRef?, ctx: JsContext?) {
        val name = x?.getName()
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
        return nameRef?.getName()?.staticRef is JsFunction
    }

    private fun wasProcessed(function: JsFunction?): Boolean = function in processed
}

