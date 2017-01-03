/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef

import org.jetbrains.kotlin.js.inline.util.collectUsedNames
import org.jetbrains.kotlin.js.inline.util.transitiveStaticRef

/**
 * Removes unused local function declarations like:
 *  var inc = _.foo.f$inc(a)
 *
 * Declaration can become unused, if inlining happened.
 */
fun removeUnusedLocalFunctionDeclarations(root: JsNode) {
    val removable =
            with(UnusedInstanceCollector()) {
                accept(root)
                removableDeclarations
            }

    NodeRemover(JsStatement::class.java) {
        it in removable
    }.accept(root)
}

private class UnusedInstanceCollector : JsVisitorWithContextImpl() {
    private val tracker = ReferenceTracker<JsName, JsStatement>()

    val removableDeclarations: List<JsStatement>
        get() = tracker.removable

    override fun visit(x: JsVars.JsVar, ctx: JsContext<*>): Boolean {
        if (!isLocalFunctionDeclaration(x)) return super.visit(x, ctx)

        val name = x.name!!
        val statementContext = lastStatementLevelContext
        val currentStatement = statementContext.currentNode
        tracker.addCandidateForRemoval(name, currentStatement!!)

        val references = collectUsedNames(x)
        references.filterNotNull()
                .forEach { tracker.addRemovableReference(name, it) }

        return false
    }

    override fun visit(x: JsNameRef, ctx: JsContext<*>): Boolean {
        var q: JsNameRef? = x
        while (q != null) {
            q.name?.let { tracker.markReachable(it) }
            q = q.qualifier as? JsNameRef
        }
        return false
    }

    private fun isLocalFunctionDeclaration(jsVar: JsVars.JsVar): Boolean {
        val name = jsVar.name
        val expr = jsVar.initExpression

        // For the case like this: `b = a; c = b;`, where `a` is a function. In this case we should remove both declaration,
        // although second one contains 'usage' of `b`.
        // see `inlineEvaluationOrder/cases/lambdaWithClosure.kt`.
        if (expr is JsNameRef && (expr.name?.let { tracker.isReferenceToRemovableCandidate(it) } ?: false)) return true

        val staticRef = name?.staticRef
        return staticRef != null && staticRef == expr && isFunctionReference(expr)
    }
}

// For RHS of `var a = b;` checks whether *b* is a reference to a function or a closure instantiation, direct or indirect.
private fun isFunctionReference(expr: JsExpression): Boolean {
    val qualifier = when (expr) {
        // `var tmp = foo(closure)`, where `foo` is a closure constructor.
        is JsInvocation -> expr.qualifier

        // Either alias to another variable that holds function or a lambda without closure.
        is JsNameRef -> expr

        else -> null
    }

    return qualifier?.transitiveStaticRef is JsFunction
}