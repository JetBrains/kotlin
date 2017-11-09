/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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