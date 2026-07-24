/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

/**
 * Marker interface for all possible generated bridges.
 */
public sealed class SirBridge(public val name: String)

/**
 * A C-like wrapper around some Kotlin function.
 * Abstracts away all nuances of Kotlin compiler ABI, making it possible
 * to call Kotlin code from other environments.
 */
public class SirFunctionBridge(
    name: String,
    public val kotlinFunctionBridge: KotlinFunctionBridge,
    public val cDeclarationBridge: CFunctionBridge,
) : SirBridge(name) {
    override fun equals(other: Any?): Boolean {
        return other is SirFunctionBridge && name == other.name &&
                kotlinFunctionBridge.lines.firstOrNull() == other.kotlinFunctionBridge.lines.firstOrNull() &&
                cDeclarationBridge.lines.firstOrNull() == other.cDeclarationBridge.lines.firstOrNull()
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + kotlinFunctionBridge.hashCode()
        result = 31 * result + cDeclarationBridge.hashCode()
        return result
    }
}

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
 * A reverse bridge that allows Kotlin to call into Swift overrides.
 * This is the mirror of [SirFunctionBridge]: instead of Swift calling Kotlin,
 * Kotlin dispatches through this bridge to reach a Swift override.
 */
public class SirReverseFunctionBridge(
    name: String,
    public val kotlinFunctionBridge: KotlinFunctionBridge,
    public val swiftFunctionBridge: SwiftFunctionBridge,
    public val cDeclarationBridge: CFunctionBridge,
) : SirBridge(name) {
    override fun equals(other: Any?): Boolean {
        return other is SirReverseFunctionBridge && name == other.name &&
                kotlinFunctionBridge.lines.firstOrNull() == other.kotlinFunctionBridge.lines.firstOrNull() &&
                swiftFunctionBridge.lines.firstOrNull() == other.swiftFunctionBridge.lines.firstOrNull() &&
                cDeclarationBridge.lines.firstOrNull() == other.cDeclarationBridge.lines.firstOrNull()
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + kotlinFunctionBridge.hashCode()
        result = 31 * result + swiftFunctionBridge.hashCode()
        result = 31 * result + cDeclarationBridge.hashCode()
        return result
    }
}

/**
 * Swift part of [SirReverseFunctionBridge].
 *
 * @param lines definition of the Swift `@_cdecl` bridge function.
 */
public class SwiftFunctionBridge(
    public val lines: List<String>,
)

/**
 * Bridge that implements mapping from Kotlin type name to Swift type name.
 *
 * @see TypeBindingBridgeRequest
 */
public class SirTypeBindingBridge(
    name: String,
    /**
     * File-level annotation to be placed on the generated Kotlin bridge.
     */
    public val kotlinFileAnnotation: String,
    public val kotlinOptIns: List<String> = emptyList(),
) : SirBridge(name)
