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
 * `for (element : collectionOfElements) {
 *      // body
 *  }`
 *
 *  loop expression.
 */
interface UForEachExpression : ULoopExpression {
    /**
     * Returns the loop variable.
     */
    val variable: UParameter

    /**
     * Returns the iterated value (collection, sequence, iterable etc.)
     */
    val iteratedValue: UExpression

    /**
     * Returns the identifier for the 'for' ('foreach') keyword.
     */
    val forIdentifier: UIdentifier

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitForEachExpression(this)) return
        iteratedValue.accept(visitor)
        body.accept(visitor)
        visitor.afterVisitForEachExpression(this)
    }

    override fun asRenderString() = buildString {
        append("for (")
        append(variable.name)
        append(" : ")
        append(iteratedValue.asRenderString())
        append(") ")
        append(body.asRenderString())
    }

    override fun asLogString() = log("UForEachExpression", variable, iteratedValue, body)
}
