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
 * The common interface for all Uast elements.
 */
interface UElement {
    /**
     * Returns the element parent.
     */
    val containingElement: UElement?

    /**
     * Returns true if this element is valid, false otherwise.
     */
    val isPsiValid: Boolean
        get() = true

    /**
     * Returns the list of comments for this element.
     */
    val comments: List<UComment>
        get() = emptyList()

    /**
     * Returns the log string.
     *
     * Output example (should be something like this):
     * UWhileExpression
     *     UBinaryExpression (>)
     *         USimpleReferenceExpression (i)
     *         ULiteralExpression (5)
     *     UBlockExpression
     *         UCallExpression (println)
     *             ULiteralExpression (ABC)
     *         UPostfixExpression (--)
     *             USimpleReferenceExpression(i)
     *
     * @return the expression tree for this element.
     * @see [UIfExpression] for example.
     */
    fun asLogString(): String

    /**
     * Returns the string in pseudo-code.
     *
     * Output example (should be something like this):
     * while (i > 5) {
     *     println("Hello, world")
     *     i--
     * }
     *
     * @return the rendered text.
     * @see [UIfExpression] for example.
     */
    fun asRenderString(): String = asLogString()

    /**
     * Returns the string as written in the source file.
     * Use this String only for logging and diagnostic text messages.
     *
     * @return the original text.
     */
    fun asSourceString(): String = asRenderString()

    /**
     * Passes the element to the specified visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    fun accept(visitor: UastVisitor) {
        visitor.visitElement(this)
        visitor.afterVisitElement(this)
    }
}