/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.bridge.impl.BridgeGeneratorImpl
import org.jetbrains.kotlin.sir.bridge.impl.CBridgePrinter
import org.jetbrains.kotlin.sir.bridge.impl.KotlinBridgePrinter

/**
 * Description of a Kotlin function for which we are creating the bridge.
 *
 * @param function SIR function we are generating bridge for
 * @param bridgeName C name of the bridge
 */
class BridgeRequest(
    val function: SirFunction,
    val bridgeName: String,
    val fqName: List<String>,
)

/**
 * A C-like wrapper around some Kotlin function.
 * Abstracts away all nuances of Kotlin compiler ABI, making it possible
 * to call Kotlin code from other environments.
 */
class FunctionBridge(
    val kotlinFunctionBridge: KotlinFunctionBridge,
    val cDeclarationBridge: CFunctionBridge,
)

/**
 * C part of [FunctionBridgeImpl].
 *
 * @param lines with function declaration.
 * @param headerDependencies required headers.
 */
class CFunctionBridge(
    val lines: List<String>,
    val headerDependencies: List<String>,
)

/**
 * Kotlin part of [FunctionBridgeImpl].
 *
 * @param lines definition of Kotlin bridge.
 * @param packageDependencies required packages to make sources compile.
 */
class KotlinFunctionBridge(
    val lines: List<String>,
    val packageDependencies: List<String>,
)

/**
 * Generates [FunctionBridge] that binds SIR function to its Kotlin origin.
 */
interface BridgeGenerator {
    fun generate(request: BridgeRequest): FunctionBridge
}

/**
 * A common interface for classes that serialize [FunctionBridge] in some form.
 */
interface BridgePrinter {
    /**
     * Populate printer with an additional [bridge].
     */
    fun add(bridge: FunctionBridge)

    /**
     * Outputs the aggregated result.
     */
    fun print(): Sequence<String>
}

fun createBridgeGenerator(): BridgeGenerator =
    BridgeGeneratorImpl()

fun createCBridgePrinter(): BridgePrinter =
    CBridgePrinter()

fun createKotlinBridgePrinter(): BridgePrinter =
    KotlinBridgePrinter()

