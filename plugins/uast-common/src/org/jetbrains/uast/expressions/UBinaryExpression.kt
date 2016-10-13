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

import com.intellij.psi.PsiMethod
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents a binary expression (value1 op value2), eg. `2 + "A"`.
 */
interface UBinaryExpression : UExpression {
    /**
     * Returns the left operand.
     */
    val leftOperand: UExpression

    /**
     * Returns the right operand.
     */
    val rightOperand: UExpression

    /**
     * Returns the binary operator.
     */
    val operator: UastBinaryOperator

    /**
     * Returns the operator identifier.
     */
    val operatorIdentifier: UIdentifier?

    /**
     * Resolve the operator method.
     * 
     * @return the resolved method, or null if the method can't be resolved, or if the expression is not a method call.
     */
    fun resolveOperator(): PsiMethod?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitBinaryExpression(this)) return
        leftOperand.accept(visitor)
        rightOperand.accept(visitor)
        visitor.afterVisitBinaryExpression(this)
    }

    override fun asLogString() =
            "UBinaryExpression (${operator.text})" + LINE_SEPARATOR +
            leftOperand.asLogString().withMargin + LINE_SEPARATOR +
            rightOperand.asLogString().withMargin

    override fun asRenderString() = leftOperand.asRenderString() + ' ' + operator.text + ' ' + rightOperand.asRenderString()
}