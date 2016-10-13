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
 * Represents
 *
 * `if (condition) {
 *     // do if true
 * } else {
 *     // do if false
 * }`
 *
 * and
 *
 * `condition : trueExpression ? falseExpression`
 *
 * condition expressions.
 */
interface UIfExpression : UExpression {
    /**
     * Returns the condition expression.
     */
    val condition: UExpression

    /**
     * Returns the expression which is executed if the condition is true, or null if the expression is empty.
     */
    val thenExpression: UExpression?

    /**
     * Returns the expression which is executed if the condition is false, or null if the expression is empty.
     */
    val elseExpression: UExpression?

    /**
     * Returns true if the expression is ternary (condition ? trueExpression : falseExpression).
     */
    val isTernary: Boolean

    /**
     * Returns an identifier for the 'if' keyword.
     */
    val ifIdentifier: UIdentifier

    /**
     * Returns an identifier for the 'else' keyword, or null if the conditional expression has not the 'else' part.
     */
    val elseIdentifier: UIdentifier?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitIfExpression(this)) return
        condition.accept(visitor)
        thenExpression?.accept(visitor)
        elseExpression?.accept(visitor)
        visitor.afterVisitIfExpression(this)
    }

    override fun asLogString() = log("UIfExpression", condition, thenExpression, elseExpression)

    override fun asRenderString() = buildString {
        if (isTernary) {
            append("(" + condition.asRenderString() + ")")
            append(" ? ")
            append("(" + (thenExpression?.asRenderString() ?: "<noexpr>") + ")")
            append(" : ")
            append("(" + (elseExpression?.asRenderString() ?: "<noexpr>") + ")")
        } else {
            append("if (${condition.asRenderString()}) ")
            thenExpression?.let { append(it.asRenderString()) }
            val elseBranch = elseExpression
            if (elseBranch != null && elseBranch !is UastEmptyExpression) {
                if (thenExpression !is UBlockExpression) append(" ")
                append("else ")
                append(elseBranch.asRenderString())
            }
        }
    }
}
