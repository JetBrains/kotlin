/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import kotlin.annotations.ExtensionContractsDsl
import kotlin.contracts.ContractBuilder
import kotlin.contracts.ExperimentalContracts

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.provides(context: ProvidesContextDescription): ProvidesContext {
    return object : ProvidesContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.requires(context: RequiresContextDescription): RequiresContext {
    return object : RequiresContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.requiresNot(context: NotRequiresContextDescription): RequiresNotContext {
    return object : RequiresNotContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.starts(context: StartsContextDescription): StartsContext {
    return object : StartsContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.closes(context: ClosesContextDescription): ClosesContext {
    return object : ClosesContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.callsIn(block: Function<*>, context: CallsBlockInContextDescription): CallsBlockInContext {
    return object : CallsBlockInContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.expectsTo(block: Function<*>, context: BlockExpectsToContextDescription): BlockExpectsToContext {
    return object : BlockExpectsToContext {}
}

@ExtensionContractsDsl
@ExperimentalContracts
fun ContractBuilder.notExpectsTo(block: Function<*>, context: BlockNotExpectsToContextDescription): BlockNotExpectsToContext {
    return object : BlockNotExpectsToContext {}
}