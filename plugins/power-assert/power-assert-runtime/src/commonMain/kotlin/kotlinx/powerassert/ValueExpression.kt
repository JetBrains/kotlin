/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * An [Expression] for a value present in source code.
 * This is the default implementation of an [Expression]
 * used for all values not representable by other [Expression] implementations.
 */
// TODO SimpleExpression?
@ExperimentalPowerAssert
public class ValueExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(deltaOffset: Int): ValueExpression {
        return ValueExpression(
            startOffset = startOffset + deltaOffset,
            endOffset = endOffset + deltaOffset,
            displayOffset = displayOffset + deltaOffset,
            value = value,
        )
    }

    override fun toString(): String {
        return "ValueExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value)"
    }
}
