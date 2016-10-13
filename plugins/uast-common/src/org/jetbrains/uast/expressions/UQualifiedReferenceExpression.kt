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

import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents the qualified expression (receiver.selector).
 */
interface UQualifiedReferenceExpression : UReferenceExpression {
    /**
     * Returns the expression receiver.
     */
    val receiver: UExpression

    /**
     * Returns the expression selector.
     */
    val selector: UExpression

    /**
     * Returns the access type (simple, safe access, etc.).
     */
    val accessType: UastQualifiedExpressionAccessType

    override fun asRenderString() = receiver.asRenderString() + accessType.name + selector.asRenderString()

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitQualifiedReferenceExpression(this)) return
        receiver.accept(visitor)
        selector.accept(visitor)
        visitor.afterVisitQualifiedReferenceExpression(this)
    }

    override fun asLogString() = log("UQualifiedExpression", receiver, selector)
}