/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents a
 *
 * `for (initDeclarations; loopCondition; update) {
 *      // body
 *  }`
 *
 *  loop expression.
 */
interface UForExpression : ULoopExpression {
    /**
     * Returns the [UExpression] containing variable declarations, or null if the are no variables declared.
     */
    val declaration: UExpression?

    /**
     * Returns the loop condition, or null if the condition is empty.
     */
    val condition: UExpression?

    /**
     * Returns the loop update expression(s).
     */
    val update: UExpression?

    /**
     * Returns the identifier for the 'for' keyword.
     */
    val forIdentifier: UIdentifier

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitForExpression(this)) return
        declaration?.accept(visitor)
        condition?.accept(visitor)
        update?.accept(visitor)
        body.accept(visitor)
        visitor.afterVisitForExpression(this)
    }

    override fun asRenderString() = buildString {
        append("for (")
        declaration?.let { append(it.asRenderString()) }
        append("; ")
        condition?.let { append(it.asRenderString()) }
        append("; ")
        update?.let { append(it.asRenderString()) }
        append(") ")
        append(body.asRenderString())
    }

    override fun asLogString() = log("UForExpression", declaration, condition, update, body)
}
