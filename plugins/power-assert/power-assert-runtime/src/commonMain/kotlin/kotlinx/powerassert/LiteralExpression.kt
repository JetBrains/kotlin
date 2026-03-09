/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * An [Expression] for a literal value present in source code.
 * This is different from a constant, as access of a `const val` will be represented as a [ValueExpression].
 * Possible literals include numbers, strings, booleans, and null.
 */
@ExperimentalPowerAssert
public class LiteralExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(deltaOffset: Int): LiteralExpression {
        return LiteralExpression(
            startOffset = startOffset + deltaOffset,
            endOffset = endOffset + deltaOffset,
            displayOffset = displayOffset + deltaOffset,
            value = value,
        )
    }

    override fun toString(): String {
        return "LiteralExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value)"
    }
}
