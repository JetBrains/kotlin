/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType

/**
 * Tries to find the superclass [KaClassSymbol] symbol which is *not* kotlin.Any
 *
 * e.g.
 * ```kotlin
 * abstract class A
 *
 * class B: A
 *
 * fun example() {
 *     val symbolOfB = // ...
 *     val symbolOfA = symbolOfB.getSuperClassSymbolNotAny()
 * }
 * ```
 */
internal fun KaSession.getSuperClassSymbolNotAny(symbol: KaClassSymbol): KaClassSymbol? {
    return getSuperClassTypeNotAny(symbol)?.expandedSymbol
}

/**
 * Tries to find the supertype of this [KaClassSymbol] which is a superclass (not Any)
 * ```kotlin
 * abstract class A
 *
 * class B: A
 *
 * fun example() {
 *     val symbolOfB = // ...
 *     val typeRepresentingA = symbolOfB.getSuperClassTypeNotAny()
 * }
 * ```
 */
internal fun KaSession.getSuperClassTypeNotAny(symbol: KaClassSymbol): KaClassType? {
    return symbol.superTypes.firstNotNullOfOrNull find@{ superType ->
        if (superType.isAnyType || superType.isError) return@find null
        if (superType is KaClassType) {
            val classSymbol = superType.expandedSymbol ?: return@find null
            if (classSymbol.classKind.isClass) {
                return superType
            }
        }
        null
    }
}
