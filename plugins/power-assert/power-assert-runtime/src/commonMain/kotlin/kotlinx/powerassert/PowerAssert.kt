/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

import kotlin.jvm.JvmStatic

/**
 * Annotates a function which natively supports Power-Assert call transformation.
 * Calls to this function, when transformed with the Power-Assert compiler plugin,
 * will provide access to an instance of [CallExplanation] via [PowerAssert.explanation].
 * This explanation will provide information about the call-site so a Power-Assert style
 * diagram can be generated.
 *
 * ```
 * @PowerAssert
 * fun assert(condition: Boolean) {
 *     if (!condition) {
 *         val explanation = PowerAssert.explanation
 *         throw AssertionError(explanation?.toDefaultMessage() ?: "Assertion failed")
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@ExperimentalPowerAssert
public annotation class PowerAssert {
    public companion object {
        /**
         * Provides access to call-site information where the function was called as a [CallExplanation].
         * May only be accessed from within a function annotated with [PowerAssert].
         */
        @Suppress("RedundantNullableReturnType")
        @JvmStatic
        public val explanation: CallExplanation?
            get() = throw NotImplementedError("Intrinsic property! Make sure the Power-Assert compiler plugin is applied to your build.")
    }

    /**
     * Indicates the Power-Assert compiler plugin should ignore the annotated element.
     *
     * A parameter of a function may be annotated, so call-site information is never provided about the argument.
     *
     * ```
     * @PowerAssert
     * fun assert(
     *     condition: Boolean,
     *     // Parameter `message` will not be included in generated explanation at call-site.
     *     @PowerAssert.Ignore message: String? = null,
     * )
     * ```
     *
     * Or a type may be annotated, so function arguments of that type are automatically ignored.
     *
     * ```
     * @PowerAssert.Ignore // Parameters of type AssertionBuilder are automatically ignored.
     * class AssertionBuilder<T>(val subject: T)
     *
     * @PowerAssert
     * fun AssertionBuilder<*>.isEqualTo(value: Any?)
     * ```
     */
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    @MustBeDocumented
    public annotation class Ignore
}
