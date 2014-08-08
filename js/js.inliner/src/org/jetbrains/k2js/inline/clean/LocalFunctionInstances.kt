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

import java.util.ArrayList
import java.util.IdentityHashMap

public fun removeUnusedLocalFunctionInstances(root: JsNode) {
    val removable =
            with(UnusedInstanceCollector()) {
                accept(root)
                removableInstances
            }

    GenericRemover(removable).accept(root)
}

private class UnusedInstanceCollector : JsVisitorWithContextImpl() {
    private val tracker = ReferenceTracker<JsName, JsStatement>()

    public val removableInstances: List<JsStatement>
        get() = tracker.removable

    override fun visit(x: JsVars.JsVar?, ctx: JsContext?): Boolean {
        if (x == null) return false

        if (!isLocalFunctionInstance(x)) return super.visit(x, ctx)

        val name = x.getName()!!
        val statementContext = getLastStatementLevelContext()
        val currentNode = statementContext.getCurrentNode()
        assert(currentNode is JsStatement) { "expected context containing statement" }
        val currentStatement = currentNode as JsStatement
        tracker.addCandidateForRemoval(name, currentStatement)

        val references = collectReferencesInside(x)
        references.filterNotNull()
                  .forEach { tracker.addRemovableReference(name, it) }

        return false
    }

    override fun visit(x: JsNameRef?, ctx: JsContext?): Boolean {
        val name = x?.getName()

        if (name != null) {
            tracker.markReachable(name)
        }

        return false
    }

    private fun isLocalFunctionInstance(jsVar: JsVars.JsVar): Boolean {
        val name = jsVar.getName()
        val expr = jsVar.getInitExpression()
        val staticRef = name?.getStaticRef()

        return staticRef != null && staticRef == expr
    }
}