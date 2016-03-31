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

import org.jetbrains.uast.visitor.UastVisitor

interface UUnaryExpression : UExpression {
    val operand: UExpression
    val operator: UastOperator

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitUnaryExpression(this)) return
        operand.accept(visitor)
        visitor.afterVisitUnaryExpression(this)
    }
}

interface UPrefixExpression : UUnaryExpression {
    override val operator: UastPrefixOperator

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitPrefixExpression(this)) return
        operand.accept(visitor)
        visitor.afterVisitPrefixExpression(this)
    }

    override fun logString() = log("UPrefixExpression (${operator.text})", operand)
    override fun renderString() = operator.text + operand.renderString()
}

interface UPostfixExpression : UUnaryExpression {
    override val operator: UastPostfixOperator

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitPostfixExpression(this)) return
        operand.accept(visitor)
        visitor.afterVisitPostfixExpression(this)
    }

    override fun logString() = log("UPostfixExpression (${operator.text})", operand)
    override fun renderString() = operand.renderString() + operator.text
}