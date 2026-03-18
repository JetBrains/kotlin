/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * Provides information about a function call, including its source and arguments.
 */
// TODO CallArgumentsExplanation?
@ExperimentalPowerAssert
public class CallExplanation(
    override val offset: Int,
    override val source: String,

    /**
     * The arguments provided to the function call in parameter order.
     * Implicit, default, or arguments annotated with [PowerAssert.Ignore] will be `null`.
     */
    public val arguments: List<Argument?>,
) : Explanation() {
    override val expressions: List<Expression>
        get() = arguments.sortedBy { it?.startOffset }.flatMap { it?.expressions.orEmpty() }

    override fun toString(): String {
        return "CallExplanation(offset=$offset, source='$source', arguments=$arguments)"
    }

    /**
     * Provides information about an argument to a function call.
     */
    public class Argument(
        /**
         * The text character, within [Explanation.source], where the argument source code begins (inclusive).
         */
        public val startOffset: Int,

        /**
         * The text character, within [Explanation.source], where the argument source code ends (exclusive).
         */
        public val endOffset: Int,

        /**
         * The [Kind] of argument, be it [Kind.DISPATCH], [Kind.CONTEXT], [Kind.EXTENSION], or [Kind.VALUE].
         */
        public val kind: Kind,

        /**
         * All [Expression]s which were evaluated as part of this argument.
         * Expressions are provided in evaluation order.
         */
        public val expressions: List<Expression>,
    ) {
        override fun toString(): String {
            return "Argument(startOffset=$startOffset, endOffset=$endOffset, kind=$kind, expressions=$expressions)"
        }

        public enum class Kind {
            DISPATCH,
            CONTEXT,
            EXTENSION,
            VALUE, // TODO REGULAR?
        }
    }
}
