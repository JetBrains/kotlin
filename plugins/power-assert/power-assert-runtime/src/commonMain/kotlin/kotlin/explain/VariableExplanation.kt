/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.explain

public class VariableExplanation(
    override val offset: Int,
    override val source: String,
    public val name: String,
    public val initializer: Initializer,
) : Explanation() {
    override val expressions: List<Expression>
        get() = initializer.expressions

    override fun toString(): String {
        return "VariableExplanation(offset=$offset, source='$source', name='$name', initializer=$initializer)"
    }

    public class Initializer(
        public val startOffset: Int,
        public val endOffset: Int,
        public val expressions: List<Expression>,
    ) {
        override fun toString(): String {
            return "Initializer(startOffset=$startOffset, endOffset=$endOffset, expressions=$expressions)"
        }
    }

    @ExplainIgnore
    public companion object {
        @JvmStatic
        @ExplainCall
        @Suppress("UNUSED_PARAMETER")
        public fun of(variable: Any?): VariableExplanation {
            error("Power-Assert compiler-plugin must be applied to project to use this function.")
        }

        @JvmStatic
        @Deprecated(level = DeprecationLevel.HIDDEN, message = "Manual implementation for binary compatibility.")
        @Suppress("FunctionName", "UNUSED_PARAMETER")
        public fun `of$explained`(variable: Any?, explanation: CallExplanation): VariableExplanation {
            val expression = explanation.valueArguments["variable"]?.expressions?.singleOrNull()
            require(expression is VariableAccessExpression) {
                """Parameter must be an access expression of a local variable annotated with @kotlinx.powerassert.Explain.
Expected: of(someLocalVariable)
Found:    ${explanation.source.trimIndent().replace("\n", "\n          ")}"""
            }
            return expression.explanation
        }
    }
}
