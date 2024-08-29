/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.explain

public class CallExplanation(
    override val offset: Int,
    override val source: String,
    public val dispatchReceiver: Receiver?,
    public val extensionReceiver: Receiver?,
    public val valueArguments: Map<String, ValueArgument>,
) : Explanation() {
    override val expressions: List<Expression>
        get() = buildList {
            if (dispatchReceiver != null) addAll(dispatchReceiver.expressions)
            if (extensionReceiver != null) addAll(extensionReceiver.expressions)
            for (valueArgument in valueArguments.values) {
                addAll(valueArgument.expressions)
            }
        }

    override fun toString(): String {
        return "CallExplanation(offset=$offset, source='$source', dispatchReceiver=$dispatchReceiver, extensionReceiver=$extensionReceiver, valueArguments=$valueArguments)"
    }

    public abstract class Argument internal constructor() {
        public abstract val startOffset: Int
        public abstract val endOffset: Int
        public abstract val expressions: List<Expression>
    }

    public class ValueArgument(
        override val startOffset: Int,
        override val endOffset: Int,
        override val expressions: List<Expression>,
    ) : Argument() {
        override fun toString(): String {
            return "ValueArgument(startOffset=$startOffset, endOffset=$endOffset, expressions=$expressions)"
        }
    }

    public class Receiver(
        override val startOffset: Int,
        override val endOffset: Int,
        override val expressions: List<Expression>,
    ) : Argument() {
        override fun toString(): String {
            return "Receiver(startOffset=$startOffset, endOffset=$endOffset, expressions=$expressions)"
        }
    }

    @ExplainIgnore
    public companion object {
        @JvmStatic
        @ExplainCall
        @Suppress("UNUSED_PARAMETER")
        public fun <T> of(value: T): Pair<T, CallExplanation> {
            error("Power-Assert compiler-plugin must be applied to project to use this function.")
        }

        @JvmStatic
        @Deprecated(level = DeprecationLevel.HIDDEN, message = "Manual implementation for binary compatibility.")
        @Suppress("FunctionName")
        public fun <T> `of$explained`(value: T, explanation: CallExplanation): Pair<T, CallExplanation> {
            return value to explanation
        }
    }
}
