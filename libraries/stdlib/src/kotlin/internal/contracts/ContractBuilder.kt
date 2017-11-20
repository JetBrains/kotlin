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