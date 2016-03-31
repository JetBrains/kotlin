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

/**
 * Represents a
 *
 * ` switch (expression) {
 *       case value1 -> expr1
 *       case value2 -> expr2
 *       ...
 *       else -> exprElse
 *   }
 *
 *   conditional expression.
 */
interface USwitchExpression : UExpression {
    /*
        Returns the expression on which the `switch` expression is performed.
     */
    val expression: UExpression?

    /*
        Returns the switch body.
        Body should contain [USwitchClauseExpression] expressions.
     */
    val body: UExpression

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitSwitchExpression(this)) return
        expression?.accept(visitor)
        body.accept(visitor)
        visitor.afterVisitSwitchExpression(this)
    }

    override fun logString() = log("USwitchExpression", expression, body)
    override fun renderString() = buildString {
        val expr = expression?.let { "(" + it.renderString() + ") " } ?: ""
        appendln("switch $expr")
        appendln(body.renderString())
    }
}

/**
 * Represents a [USwitchExpression] clause.
 * [USwitchClauseExpression] does not contain the clause body,
 *     and the actual body expression should be the next element in the parent expression list.
 */
interface USwitchClauseExpression : UExpression {
    /**
     * Returns the list of values for this clause, or null if the are no values for this close
     *     (for example, for the `else` clause).
     */
    val caseValues: List<UExpression>?

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitSwitchClauseExpression(this)) return
        caseValues?.acceptList(visitor)
        visitor.afterVisitSwitchClauseExpression(this)
    }

    override fun renderString() = (caseValues?.joinToString { it.renderString() } ?: "else") + " -> "
    override fun logString() = log("USwitchClauseExpression", caseValues)
}

/**
 * Represents a [USwitchExpression] clause with the body.
 * [USwitchClauseExpressionWithBody], comparing with [USwitchClauseExpression], contains the body expression.
 *
 * Implementing this interface *is the right way* to support `switch` clauses in your language.
 */
interface USwitchClauseExpressionWithBody : USwitchClauseExpression {
    /**
     * Returns the body expression for this clause.
     */
    val body: UExpression

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitSwitchClauseExpression(this)) return
        caseValues?.acceptList(visitor)
        body.accept(visitor)
        visitor.afterVisitSwitchClauseExpression(this)
    }

    override fun renderString() = (caseValues?.joinToString { it.renderString() } ?: "else") + " -> " + body.renderString()
    override fun logString() = log("USwitchClauseExpressionWithBody", caseValues, body)
}