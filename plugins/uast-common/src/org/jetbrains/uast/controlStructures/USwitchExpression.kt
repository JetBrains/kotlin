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

interface USwitchExpression : UExpression {
    val expression: UExpression?
    val body: UExpression

    override fun traverse(handler: UastHandler) {
        expression?.handleTraverse(handler)
        body.handleTraverse(handler)
    }

    override fun logString() = log("USwitchExpression", expression, body)
    override fun renderString() = buildString {
        val expr = expression?.let { "(" + it.renderString() + ") " } ?: ""
        appendln("switch $expr")
        appendln(body.renderString())
    }
}

interface USwitchClauseExpression : UExpression

interface UExpressionSwitchClauseExpression : USwitchClauseExpression {
    val caseValue: UExpression

    override fun traverse(handler: UastHandler) {
        caseValue.handleTraverse(handler)
    }

    override fun renderString() = caseValue.renderString() + " -> "
    override fun logString() = log("UExpressionSwitchClauseExpression", caseValue)
}

interface UDefaultSwitchClauseExpression : USwitchClauseExpression {
    override fun traverse(handler: UastHandler) {}
    override fun logString() = "UDefaultSwitchClause"
    override fun renderString() = "else -> "
}

class SimpleUDefaultSwitchClauseExpression(override val parent: UElement) : UDefaultSwitchClauseExpression, NoEvaluate