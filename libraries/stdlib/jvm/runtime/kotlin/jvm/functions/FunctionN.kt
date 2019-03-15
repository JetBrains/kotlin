/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.functions

import kotlin.jvm.internal.FunctionBase

/**
 * A function that takes N >= 23 arguments.
 *
 * This interface must only be used in Java sources to reference a Kotlin function type with more than 22 arguments.
 */
@SinceKotlin("1.3")
interface FunctionN<out R> : Function<R>, FunctionBase<R> {
    /**
     * Invokes the function with the specified arguments.
     *
     * Must **throw exception** if the length of passed [args] is not equal to the parameter count returned by [arity].
     *
     * @param args arguments to the function
     */
    operator fun invoke(vararg args: Any?): R

    /**
     * Returns the number of arguments that must be passed to this function.
     */
    override val arity: Int
}
