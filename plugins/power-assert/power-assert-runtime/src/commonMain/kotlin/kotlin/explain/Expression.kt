/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.explain

public abstract class Expression internal constructor(
    public val startOffset: Int,
    public val endOffset: Int,
    public val displayOffset: Int,
    public val value: Any?,
) {
    public abstract fun copy(offset: Int): Expression
}

public interface ExplainedExpression {
    public val explanation: Explanation
}

public class ValueExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(offset: Int): ValueExpression {
        return ValueExpression(
            startOffset = startOffset + offset,
            endOffset = endOffset + offset,
            displayOffset = displayOffset + offset,
            value = value,
        )
    }

    override fun toString(): String {
        return "ValueExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value)"
    }
}

public class EqualityExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
    public val lhs: Any?,
    public val rhs: Any?,
) : Expression(startOffset, endOffset, displayOffset, value) {
    override fun copy(offset: Int): EqualityExpression {
        return EqualityExpression(
            startOffset = startOffset + offset,
            endOffset = endOffset + offset,
            displayOffset = displayOffset + offset,
            value = value,
            lhs = lhs,
            rhs = rhs,
        )
    }

    override fun toString(): String {
        return "EqualityExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value, lhs=$lhs, rhs=$rhs)"
    }
}

public class VariableAccessExpression(
    startOffset: Int,
    endOffset: Int,
    displayOffset: Int,
    value: Any?,
    public override val explanation: VariableExplanation,
) : Expression(startOffset, endOffset, displayOffset, value), ExplainedExpression {
    override fun copy(offset: Int): VariableAccessExpression {
        return VariableAccessExpression(
            startOffset = startOffset + offset,
            endOffset = endOffset + offset,
            displayOffset = displayOffset + offset,
            value = value,
            explanation = explanation,
        )
    }

    override fun toString(): String {
        return "VariableAccessExpression(startOffset=$startOffset, endOffset=$endOffset, displayOffset=$displayOffset, value=$value, explanation=$explanation)"
    }
}
