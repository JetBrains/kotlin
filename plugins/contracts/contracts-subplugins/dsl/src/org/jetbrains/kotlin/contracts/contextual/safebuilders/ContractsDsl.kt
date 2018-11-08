/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.BlockExpectsToContextDescription
import org.jetbrains.kotlin.contracts.contextual.ProvidesContextDescription
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.ReceiverOf
import kotlin.reflect.KFunction

@ExperimentalContracts
@SinceKotlin("1.3")
class Calls(func: KFunction<*>, thisReference: Any) : ProvidesContextDescription

@ExperimentalContracts
@SinceKotlin("1.3")
class CallKind(func: KFunction<*>, kind: InvocationKind, receiver: ReceiverOf) : BlockExpectsToContextDescription