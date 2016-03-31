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
 * Represents a literal expression.
 */
interface ULiteralExpression : UExpression {
    /**
     * Returns the literal expression value.
     * This is basically a String, Number or null if the literal is a `null` literal.
     */
    val value: Any?

    /**
     * Returns true if the literal is a `null`-literal, false otherwise.
     */
    val isNull: Boolean

    /**
     * Returns true if the literal is a [String] literal, false otherwise.
     */
    val isString: Boolean
        get() = evaluate() is String

    /**
     * Returns true if the literal is a [Boolean] literal, false otherwise.
     */
    val isBoolean: Boolean
        get() = evaluate() is Boolean

    /**
     * Returns the string representation of the literal expression.
     *
     * @return the string representation, or "null" if the literal is a "null"-literal.
     */
    fun asString(): String {
        val value = value
        return if (value == null)
            "null"
        else
            value.toString()
    }

    override fun accept(visitor: UastVisitor) {
        visitor.visitLiteralExpression(this)
        visitor.afterVisitLiteralExpression(this)
    }

    override fun logString() = "ULiteralExpression (${asString()})"
    override fun renderString() = if (value is String) "\"$value\"" else asString()
}
