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
 * Represent a
 *
 * `do {
 *      // body
 * } while (expr)`
 *
 * loop expression.
 */
interface UDoWhileExpression : ULoopExpression {
    /**
     * Returns the loop post-condition.
     */
    val condition: UExpression

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitDoWhileExpression(this)) return
        condition.accept(visitor)
        body.accept(visitor)
        visitor.afterVisitDoWhileExpression(this)
    }

    override fun renderString() = buildString {
        append("do ")
        append(body.renderString())
        appendln("while (${condition.renderString()})")
    }

    override fun logString() = log("UDoWhileExpression", condition, body)
}
