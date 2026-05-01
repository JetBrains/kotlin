/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.powerassert

/**
 * An [Expression] for a literal value present in source code.
 * This is different from a constant, as access of a `const val` will be represented as a [ValueExpression].
 * Possible literals include numbers, strings, booleans, and null.
 */
@ExperimentalPowerAssert
public class LiteralExpression(
    override val startOffset: Int,
    override val endOffset: Int,
    override val displayOffset: Int,
    override val value: Any?,
) : Expression() {
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
