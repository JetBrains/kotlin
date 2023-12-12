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
public class BridgeRequest(
    public val function: SirFunction,
    public val bridgeName: String,
    public val fqName: List<String>,
)

/**
 * A C-like wrapper around some Kotlin function.
 * Abstracts away all nuances of Kotlin compiler ABI, making it possible
 * to call Kotlin code from other environments.
 */
public class FunctionBridge(
    public val kotlinFunctionBridge: KotlinFunctionBridge,
    public val cDeclarationBridge: CFunctionBridge,
)

/**
 * C part of [FunctionBridgeImpl].
 *
 * @param lines with function declaration.
 * @param headerDependencies required headers.
 */
public class CFunctionBridge(
    public val lines: List<String>,
    public val headerDependencies: List<String>,
)

/**
 * Kotlin part of [FunctionBridgeImpl].
 *
 * @param lines definition of Kotlin bridge.
 * @param packageDependencies required packages to make sources compile.
 */
public class KotlinFunctionBridge(
    public val lines: List<String>,
    public val packageDependencies: List<String>,
)

/**
 * Generates [FunctionBridge] that binds SIR function to its Kotlin origin.
 */
public interface BridgeGenerator {
    public fun generate(request: BridgeRequest): FunctionBridge
}

/**
 * A common interface for classes that serialize [FunctionBridge] in some form.
 */
public interface BridgePrinter {
    /**
     * Populate printer with an additional [bridge].
     */
    public fun add(bridge: FunctionBridge)

    /**
     * Outputs the aggregated result.
     */
    public fun print(): Sequence<String>
}

public fun createBridgeGenerator(): BridgeGenerator =
    BridgeGeneratorImpl()

public fun createCBridgePrinter(): BridgePrinter =
    CBridgePrinter()

public fun createKotlinBridgePrinter(): BridgePrinter =
    KotlinBridgePrinter()

