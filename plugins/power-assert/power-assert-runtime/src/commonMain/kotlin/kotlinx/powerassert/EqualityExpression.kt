/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * An [Expression] for an equality operator invocation.
 * This is limited exclusively to the positive equality operaator (`==`)
 * and not the negative equality operator (`!=`),
 * the positive identity operator (`===`),
 * or the negaitve identity operator (`!==`).
 */
// TODO name something like PositiveEqualityOperatorExpression?
//  - may want an expression in the future to support all 4 equality/identity cases.
@ExperimentalPowerAssert
public class EqualityExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,

    /**
     * The left-hand side of the equality operator.
     */
    public val lhs: Any?,

    /**
     * The right-hand side of the equality operator.
     */
    public val rhs: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(deltaOffset: Int): EqualityExpression {
        return EqualityExpression(
            startOffset = startOffset + deltaOffset,
            endOffset = endOffset + deltaOffset,
            displayOffset = displayOffset + deltaOffset,
            value = value,
            lhs = lhs,
            rhs = rhs,
        )
    }

    override fun toString(): String {
        return "EqualityExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value, lhs=$lhs, rhs=$rhs)"
    }
}
