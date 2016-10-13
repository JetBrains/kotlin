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
 * Represents a `continue` expression.
 */
interface UContinueExpression : UExpression {
    /**
     * Returns the expression label, or null if the label is not specified.
     */
    val label: String?

    override fun accept(visitor: UastVisitor) {
        visitor.visitContinueExpression(this)
        visitor.afterVisitContinueExpression(this)
    }

    override fun asLogString() = "UContinueExpression (" + (label ?: "<no label>") + ")"
    override fun asRenderString() = label?.let { "continue@$it" } ?: "continue"
}