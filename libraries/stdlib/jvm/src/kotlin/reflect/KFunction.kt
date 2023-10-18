/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Represents a function with introspection capabilities.
 */
@Suppress("ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING", "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING") // Can be dropped after bootstrap update
public actual interface KFunction<out R> : KCallable<R>, Function<R> {
    /**
     * `true` if this function is `inline`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/inline-functions.html)
     * for more information.
     */
    @Suppress("NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING") // Can be dropped after bootstrap update
    @SinceKotlin("1.1")
    public val isInline: Boolean

    /**
     * `true` if this function is `external`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-interop.html#using-jni-with-kotlin)
     * for more information.
     */
    @Suppress("NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING") // Can be dropped after bootstrap update
    @SinceKotlin("1.1")
    public val isExternal: Boolean

    /**
     * `true` if this function is `operator`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/operator-overloading.html)
     * for more information.
     */
    @Suppress("NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING") // Can be dropped after bootstrap update
    @SinceKotlin("1.1")
    public val isOperator: Boolean

    /**
     * `true` if this function is `infix`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/functions.html#infix-notation)
     * for more information.
     */
    @Suppress("NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING") // Can be dropped after bootstrap update
    @SinceKotlin("1.1")
    public val isInfix: Boolean

    /**
     * `true` if this is a suspending function.
     */
    @SinceKotlin("1.1")
    public override val isSuspend: Boolean
}
