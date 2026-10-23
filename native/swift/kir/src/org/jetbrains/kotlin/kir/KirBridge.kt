/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir

/**
 * Marker interface for all possible generated bridges.
 */
sealed class KirBridge(val name: String)

/**
 * A C-like wrapper around some Swift function.
 * Kotlin calls the bridge through its Kotlin declaration, which is bound to the C declaration,
 * and the C declaration is implemented in Swift by a `@_cdecl` function that calls the original one.
 */
class KirFunctionBridge(
    name: String,
    val kotlinFunctionBridge: KotlinFunctionBridge,
    val swiftFunctionBridge: SwiftFunctionBridge,
    val cDeclarationBridge: CFunctionBridge,
) : KirBridge(name) {
    override fun equals(other: Any?): Boolean {
        return other is KirFunctionBridge && name == other.name &&
                kotlinFunctionBridge == other.kotlinFunctionBridge &&
                swiftFunctionBridge == other.swiftFunctionBridge &&
                cDeclarationBridge == other.cDeclarationBridge
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
 * Kotlin part of [KirFunctionBridge].
 *
 * @param lines declaration of the Kotlin bridge, e.g., `@ImportedBridge("...") internal external fun ...`.
 * @param packageDependencies required packages to make sources compile.
 */
class KotlinFunctionBridge(
    val lines: List<String>,
    val packageDependencies: List<String>,
) {
    override fun equals(other: Any?): Boolean {
        return other is KotlinFunctionBridge && lines == other.lines && packageDependencies == other.packageDependencies
    }

    override fun hashCode(): Int {
        var result = lines.hashCode()
        result = 31 * result + packageDependencies.hashCode()
        return result
    }
}

/**
 * Swift part of [KirFunctionBridge].
 *
 * @param lines definition of the Swift `@_cdecl` bridge function.
 */
class SwiftFunctionBridge(
    val lines: List<String>,
) {
    override fun equals(other: Any?): Boolean {
        return other is SwiftFunctionBridge && lines == other.lines
    }

    override fun hashCode(): Int = lines.hashCode()
}

/**
 * C part of [KirFunctionBridge].
 *
 * @param lines with function declaration.
 * @param headerDependencies required headers.
 */
class CFunctionBridge(
    val lines: List<String>,
    val headerDependencies: List<String>,
) {
    override fun equals(other: Any?): Boolean {
        return other is CFunctionBridge && lines == other.lines && headerDependencies == other.headerDependencies
    }

    override fun hashCode(): Int {
        var result = lines.hashCode()
        result = 31 * result + headerDependencies.hashCode()
        return result
    }
}
