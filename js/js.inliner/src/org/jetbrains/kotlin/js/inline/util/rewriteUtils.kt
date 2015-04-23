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

package org.jetbrains.kotlin.js.inline.util

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.js.inline.util.rewriters.NameReplacingVisitor
import org.jetbrains.kotlin.js.inline.util.rewriters.ReturnReplacingVisitor
import org.jetbrains.kotlin.js.inline.util.rewriters.ThisReplacingVisitor

import java.util.IdentityHashMap

public fun <T : JsNode> replaceNames(node: T, replaceMap: IdentityHashMap<JsName, JsExpression>): T {
    return NameReplacingVisitor(replaceMap).accept(node)!!
}

public fun replaceReturns(scope: JsBlock, resultRef: JsNameRef?, breakLabel: JsNameRef?): JsNode {
    val visitor = ReturnReplacingVisitor(resultRef, breakLabel)
    val withReturnReplaced = visitor.accept(scope)!!

    if (breakLabel != null) {
        val statements = scope.getStatements()
        val last = statements.last() as? JsBreak

        if (last?.getLabel()?.getName() === breakLabel.getName()) {
            statements.remove(statements.lastIndex)
        }
    }

    return withReturnReplaced
}

public fun replaceThisReference<T : JsNode>(node: T, replacement: JsExpression) {
    ThisReplacingVisitor(replacement).accept(node)
}
