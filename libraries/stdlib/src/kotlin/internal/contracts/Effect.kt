/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.contracts

import kotlin.internal.ContractsDsl

@ContractsDsl
@SinceKotlin("1.2")
internal interface Effect

@ContractsDsl
@SinceKotlin("1.2")
internal interface ConditionalEffect : Effect

@ContractsDsl
@SinceKotlin("1.2")
internal interface SimpleEffect {
    @ContractsDsl
    infix fun implies(booleanExpression: Boolean): ConditionalEffect
}


@ContractsDsl
@SinceKotlin("1.2")
internal interface Returns : SimpleEffect

@ContractsDsl
@SinceKotlin("1.2")
internal interface ReturnsNotNull : SimpleEffect

@ContractsDsl
@SinceKotlin("1.2")
internal interface CallsInPlace : SimpleEffect