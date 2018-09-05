/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl
import kotlin.internal.InlineOnly

@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@Experimental
public annotation class ExperimentalContracts

@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ContractBuilder {
    @ContractsDsl public fun returns(): Returns
    @ContractsDsl public fun returns(value: Any?): Returns
    @ContractsDsl public fun returnsNotNull(): ReturnsNotNull
    @ContractsDsl public fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace
}

@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public enum class InvocationKind {
    @ContractsDsl AT_MOST_ONCE,
    @ContractsDsl AT_LEAST_ONCE,
    @ContractsDsl EXACTLY_ONCE,
    @ContractsDsl UNKNOWN
}

@ContractsDsl
@ExperimentalContracts
@InlineOnly
@SinceKotlin("1.3")
@Suppress("UNUSED_PARAMETER")
public inline fun contract(builder: ContractBuilder.() -> Unit) { }