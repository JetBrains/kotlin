/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.explain

/**
 * When applied to a local variable, a [VariableExplanation] of the variable will be included within
 * any [CallExplanation] which accesses that local variable within an argument. When applied to a
 * function, all valid local variables within the function will inherit this annotation.
 *
 * This annotation may only be applied to local variables which are immutable and have an
 * initializer. It is a compiler error otherwise.
 *
 * When both [Explain] and [ExplainIgnore] are applied to a local variable - either explicitly or
 * implicitly - [ExplainIgnore] will take precedence.
 */
// TODO support meta-annotations? @ExplainTest?
@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class Explain

/**
 * ```
 * @ExplainCall
 * fun assert(
 *     condition: Boolean,
 *     // Parameter `message` will not have an explanation generated at call-site.
 *     @ExplainIgnore message: String? = null,
 * )
 * ```
 *
 * ```
 * @ExplainIgnore // Parameters of type AssertionBuilder are automatically ignored.
 * class AssertionBuilder<T>(val subject: T)
 *
 * @ExplainCall
 * fun AssertionBuilder<*>.isEqualTo(value: Any?)
 * ```
 *
 * ```
 * @Test @Explain
 * fun test() {
 *     // Can override function-level default to ignore specific local variables.
 *     @ExplainIgnore val repository = UserRepository()
 *     val users = repository.getUsers()
 *     assert(users.isNotEmpty())
 * }
 * ```
 *
 * When both [Explain] and [ExplainIgnore] are applied to a local variable - either explicitly or
 * implicitly - [ExplainIgnore] will take precedence.
 */
// TODO nested class within Explain? Explain.Ignore?
// TODO plugin supports configurable annotations (Spring-Boot might be good use case)
// TODO support meta-annotations?
// TODO should temporary variables of types annotated with @ExplainIgnore be considered pseudo-constants?
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ExplainIgnore

/**
 * ```
 * @ExplainCall
 * fun assert(condition: Boolean) {
 *     if (!condition) {
 *         val explanation = ExplainCall.explanation
 *         throw AssertionError(explanation?.toDefaultMessage() ?: "Assertion failed")
 *     }
 * }
 * ```
 */
// TODO nested class within Explain? Explain.Call?
// TODO name based on something else? ExplainArguments? ExplainCallSite?
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ExplainCall {
    public companion object {
        @Suppress("RedundantNullableReturnType")
        @JvmStatic
        public val explanation: CallExplanation?
            get() = throw NotImplementedError("Intrinsic property! Make sure the Power-Assert compiler-plugin is applied to your build.")
    }

}
