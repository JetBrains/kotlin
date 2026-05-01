/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.powerassert

/**
 * An [Expression] for a value present in source code.
 * This is the default implementation of an [Expression]
 * used for all values not representable by other [Expression] implementations.
 */
@ExperimentalPowerAssert
public class ValueExpression(
    override val startOffset: Int,
    override val endOffset: Int,
    override val displayOffset: Int,
    override val value: Any?,
) : Expression() {
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
