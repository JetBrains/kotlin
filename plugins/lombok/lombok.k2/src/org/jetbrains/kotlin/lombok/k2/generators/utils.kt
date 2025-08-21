package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.utils.AccessorNames
import org.jetbrains.kotlin.lombok.utils.toPropertyName
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun FirJavaField.toAccessorBaseName(config: ConeLombokAnnotations.Accessors): String? {
    val isPrimitiveBoolean = returnTypeRef.isPrimitiveBoolean()
    return if (config.prefix.isEmpty()) {
        val prefixes = if (isPrimitiveBoolean) listOf(AccessorNames.IS) else emptyList()
        toPropertyName(name.identifier, prefixes)
    } else {
        val id = name.identifier
        val name = toPropertyName(id, config.prefix)
        name.takeIf { it.length != id.length}
    }
}

fun FirTypeRef.isPrimitiveBoolean(): Boolean {
    return when (this) {
        is FirJavaTypeRef -> (type as? JavaPrimitiveType)?.type == PrimitiveType.BOOLEAN
        else -> this.coneTypeOrNull?.lowerBoundIfFlexible()?.isBoolean ?: false
    }
}

@OptIn(ExperimentalContracts::class)
fun FirClassSymbol<*>.isSuitableJavaClass(): Boolean {
    contract {
        returns(true) implies (this@isSuitableJavaClass is FirRegularClassSymbol)
    }
    return (this is FirRegularClassSymbol) && origin == FirDeclarationOrigin.Java.Source
}

@OptIn(SymbolInternals::class)
@DirectDeclarationsAccess
fun List<FirFunction>.filterClashingDeclarations(classSymbol: FirClassSymbol<*>): List<FirFunctionSymbol<*>> {
    @Suppress("UNCHECKED_CAST")
    val allStaticFunctionsAndConstructors = classSymbol.fir.declarations.filterIsInstance<FirFunction>().toMutableList()
    val result = mutableListOf<FirFunction>()
    for (function in this) {
        if (allStaticFunctionsAndConstructors.none { sameSignature(it, function) }) {
            allStaticFunctionsAndConstructors += function
            result += function
        }
    }
    return result.map { it.symbol }
}

/**
 * Lombok treat functions as having the same signature by arguments count only
 * Corresponding code in lombok - https://github.com/projectlombok/lombok/blob/v1.18.20/src/core/lombok/javac/handlers/JavacHandlerUtil.java#L752
 */
private fun sameSignature(a: FirFunction, b: FirFunction): Boolean {
    if (a is FirConstructor && b !is FirConstructor || a !is FirConstructor && b is FirConstructor) return false
    if (a.symbol.callableId.callableName != b.symbol.callableId.callableName) return false
    val aVararg = a.valueParameters.any { it.isVararg }
    val bVararg = b.valueParameters.any { it.isVararg }
    val aSize = a.valueParameters.size
    val bSize = b.valueParameters.size
    return aVararg && bVararg ||
            aVararg && bSize >= (aSize - 1) ||
            bVararg && aSize >= (bSize - 1) ||
            aSize == bSize
}

internal inline fun <A, B, C> uncurry(crossinline f: (A, B) -> C): (Pair<A, B>) -> C = { (a, b) -> f(a, b) }
