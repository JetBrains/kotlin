/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import kotlin.annotations.ExtensionContractsDsl
import kotlin.contracts.Effect
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface ProvidesContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface CallsBlockInContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface RequiresContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface BlockExpectsToContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface NotRequiresContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface BlockNotExpectsToContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface StartsContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface ClosesContextDescription

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface ProvidesContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface CallsBlockInContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface RequiresContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface BlockExpectsToContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface RequiresNotContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface BlockNotExpectsToContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface StartsContext : Effect

@ExperimentalContracts
@ExtensionContractsDsl
@SinceKotlin("1.3")
interface ClosesContext : Effect