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

package org.jetbrains.k2js.inline.util.rewriters

import com.google.dart.compiler.backend.js.ast.JsBreak
import com.google.dart.compiler.backend.js.ast.JsContext
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsExpressionStatement
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral
import com.google.dart.compiler.backend.js.ast.JsReturn
import com.google.dart.compiler.backend.js.ast.JsStatement
import com.google.dart.compiler.backend.js.ast.JsVisitorWithContextImpl
import org.jetbrains.k2js.inline.util.canHaveSideEffect
import org.jetbrains.k2js.translate.utils.JsAstUtils

class ReturnReplacingVisitor(private val resultRef: JsNameRef?, private val breakLabel: JsNameRef?) : JsVisitorWithContextImpl() {

    /**
     * Prevents replacing returns in object literal
     */
    override fun visit(x: JsObjectLiteral?, ctx: JsContext?): Boolean = false

    /**
     * Prevents replacing returns in inner function
     */
    override fun visit(x: JsFunction?, ctx: JsContext?): Boolean = false

    override fun endVisit(x: JsReturn?, ctx: JsContext?) {
        if (x == null || ctx == null) return

        if (breakLabel != null) {
            ctx.insertAfter(JsBreak(breakLabel))
        }

        val returnReplacement = getReturnReplacement(x.getExpression())
        if (returnReplacement != null) {
            ctx.insertBefore(JsExpressionStatement(returnReplacement))
        }

        ctx.removeMe()
    }

    private fun getReturnReplacement(returnExpression: JsExpression?): JsExpression? {
        if (returnExpression != null) {
            if (resultRef != null)
                return JsAstUtils.assignment(resultRef, returnExpression)

            if (canHaveSideEffect(returnExpression))
                return returnExpression
        }

        return null
    }
}