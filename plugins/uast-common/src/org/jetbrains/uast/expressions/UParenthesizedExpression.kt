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
 * Represents a parenthesized expression, e.g. `(23 + 3)`.
 */
interface UParenthesizedExpression : UExpression {
    /**
     * Returns an expression inside the parenthesis.
     */
    val expression: UExpression

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitParenthesizedExpression(this)) return
        expression.accept(visitor)
        visitor.afterVisitParenthesizedExpression(this)
    }

    override fun evaluate() = expression.evaluate()

    override fun asLogString() = log("UParenthesizedExpression", expression)
    override fun asRenderString() = '(' + expression.asRenderString() + ')'
}