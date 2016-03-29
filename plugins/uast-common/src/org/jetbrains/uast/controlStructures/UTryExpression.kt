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

interface UTryExpression : UExpression {
    val resources: List<UElement>?
    val tryClause: UExpression
    val catchClauses: List<UCatchClause>
    val finallyClause: UExpression?

    override fun traverse(callback: UastCallback) {
        resources?.handleTraverseList(callback)
        tryClause.handleTraverse(callback)
        catchClauses.handleTraverseList(callback)
        finallyClause?.handleTraverse(callback)
    }

    override fun renderString() = buildString {
        append("try ")
        appendln(tryClause.renderString())
        catchClauses.forEach { appendln(it.renderString()) }
        finallyClause?.let { append("finally ").append(it.renderString()) }
    }

    override fun logString() = "UTryExpression\n" +
            tryClause.logString().withMargin +
            catchClauses.joinToString("\n") { it.logString().withMargin } +
            (finallyClause?.let { it.logString().withMargin } ?: "<no finally clause>" )
}

interface UCatchClause : UElement {
    val body: UExpression
    val parameters: List<UVariable>
    val types: List<UType>

    override fun traverse(callback: UastCallback) {
        body.handleTraverse(callback)
        parameters.handleTraverseList(callback)
        types.handleTraverseList(callback)
    }

    override fun logString() = log("UCatchClause", body)
    override fun renderString() = "catch (e) " + body.renderString()
}