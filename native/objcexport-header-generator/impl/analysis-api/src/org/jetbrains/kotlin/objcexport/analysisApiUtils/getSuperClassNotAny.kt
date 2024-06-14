/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType

/**
 * Tries to find the superclass [KtClassOrObjectSymbol] symbol which is *not* kotlin.Any
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
context(KaSession)
internal fun KaClassOrObjectSymbol.getSuperClassSymbolNotAny(): KaClassOrObjectSymbol? {
    return getSuperClassTypeNotAny()?.expandedSymbol
}

/**
 * Tries to find the supertype of this [KtClassOrObjectSymbol] which is a superclass (not Any)
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
context(KaSession)
internal fun KaClassOrObjectSymbol.getSuperClassTypeNotAny(): KaClassType? {
    return superTypes.firstNotNullOfOrNull find@{ superType ->
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
