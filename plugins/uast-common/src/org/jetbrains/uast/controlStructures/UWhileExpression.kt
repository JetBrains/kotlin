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
 * `while (condition) {
 *      // body
 *  }`
 *
 *  expression.
 */
interface UWhileExpression : ULoopExpression {
    /**
     * Returns the loop condition.
     */
    val condition: UExpression

    /**
     * Returns an identifier for the 'while' keyword.
     */
    val whileIdentifier: UIdentifier

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitWhileExpression(this)) return
        condition.accept(visitor)
        body.accept(visitor)
        visitor.afterVisitWhileExpression(this)
    }

    override fun asRenderString() = buildString {
        append("while (${condition.asRenderString()}) ")
        append(body.asRenderString())
    }

    override fun asLogString() = log("UWhileExpression", condition, body)
}
