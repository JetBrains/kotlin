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
 * Represents a binary expression with type (value op type), e.g. ("A" instanceof String).
 */
interface UBinaryExpressionWithType : UExpression {
    /**
     * Returns the operand expression.
     */
    val operand: UExpression

    /**
     * Returns the operation kind.
     */
    val operationKind: UastBinaryExpressionWithTypeKind

    /**
     * Returns the type.
     */
    val type: UType

    /**
     * Returns the type reference.
     */
    val typeReference: UTypeReference?

    override fun logString() = log("UBinaryExpressionWithType (${getExpressionType()?.name}, ${operationKind.name})", operand)
    override fun renderString() = "${operand.renderString()} ${operationKind.name} ${type.name}"

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitBinaryExpressionWithType(this)) return
        operand.accept(visitor)
        type.accept(visitor)
        visitor.afterVisitBinaryExpressionWithType(this)
    }
}