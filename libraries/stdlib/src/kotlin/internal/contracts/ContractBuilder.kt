/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.contracts

import kotlin.internal.ContractsDsl
import kotlin.internal.InlineOnly

@ContractsDsl
@SinceKotlin("1.2")
internal interface ContractBuilder {
    @ContractsDsl fun returns(): Returns
    @ContractsDsl fun returns(value: Any?): Returns
    @ContractsDsl fun returnsNotNull(): ReturnsNotNull
    @ContractsDsl fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace
}

@ContractsDsl
@SinceKotlin("1.2")
internal enum class InvocationKind {
    @ContractsDsl AT_MOST_ONCE,
    @ContractsDsl AT_LEAST_ONCE,
    @ContractsDsl EXACTLY_ONCE,
    @ContractsDsl UNKNOWN
}

@ContractsDsl
@InlineOnly
@SinceKotlin("1.2")
internal inline fun contract(builder: ContractBuilder.() -> Unit) { }