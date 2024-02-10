/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType

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
context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.getSuperClassSymbolNotAny(): KtClassOrObjectSymbol? {
    return getSuperClassTypeNotAny()?.expandedClassSymbol
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
context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.getSuperClassTypeNotAny(): KtNonErrorClassType? {
    return superTypes.firstNotNullOfOrNull find@{ superType ->
        if (superType.isAny || superType.isError) return@find null
        if (superType is KtNonErrorClassType) {
            val classSymbol = superType.expandedClassSymbol ?: return@find null
            if (classSymbol.classKind.isClass) {
                return superType
            }
        }
        null
    }
}
