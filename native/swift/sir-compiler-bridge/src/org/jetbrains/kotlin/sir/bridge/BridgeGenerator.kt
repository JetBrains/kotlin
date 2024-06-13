/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.impl.*

/**
 * Description of a Kotlin callable for which we are creating the bridge.
 */
public class BridgeRequest(
    /**
     * SIR callable we are generating bridge for.
     */
    public val callable: SirCallable,
    /**
     * Prefix of the bridge's C name.
     */
    public val bridgeName: String,
    /**
     * Fully Qualified Name of Kotlin callable.
     */
    public val fqName: List<String>,
) : Comparable<BridgeRequest> {
    public override fun compareTo(other: BridgeRequest): Int {
        return bridgeName.compareTo(other.bridgeName)
    }
}

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
 * Generates [FunctionBridge] and [SirFunctionBody] that binds SIR function to its Kotlin origin.
 */
public interface BridgeGenerator {
    public fun generateFunctionBridges(request: BridgeRequest): List<FunctionBridge>
    public fun generateSirFunctionBody(request: BridgeRequest): SirFunctionBody
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

public interface SirTypeNamer {
    public fun swiftFqName(type: SirType): String
    public fun kotlinFqName(type: SirType): String
}

public fun createBridgeGenerator(namer: SirTypeNamer): BridgeGenerator =
    BridgeGeneratorImpl(namer)

public fun createCBridgePrinter(): BridgePrinter =
    CBridgePrinter()

public fun createKotlinBridgePrinter(): BridgePrinter =
    KotlinBridgePrinter()

