/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationTarget.*
import kotlin.experimental.ExperimentalTypeInference

/**
 * Allows to infer generic type arguments of a function from the calls in the annotated function parameter of that function.
 *
 * When this annotation is placed on a generic function parameter of a function,
 * it enables to infer the type arguments of that generic function from the lambda body passed to that parameter.
 *
 * The calls that affect inference are either members of the receiver type of an annotated function parameter or
 * extensions for that type. The extensions must be themselves annotated with `@BuilderInference`.
 *
 * Example: we declare
 * ```
 * fun <T> sequence(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Sequence<T>
 * ```
 * and use it like
 * ```
 * val result = sequence { yield("result") }
 * ```
 * Here the type argument of the resulting sequence is inferred to `String` from
 * the argument of the [SequenceScope.yield] function, that is called inside the lambda passed to [sequence].
 *
 * Note: this annotation is experimental, see [ExperimentalTypeInference] on how to opt-in for it.
 */
@Target(VALUE_PARAMETER, FUNCTION, PROPERTY)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@ExperimentalTypeInference
public annotation class BuilderInference
