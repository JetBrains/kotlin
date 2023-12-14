/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier

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
    return superTypes.firstNotNullOfOrNull find@{ superType ->
        if (superType.isAny) return@find null
        if (superType is KtClassType) {
            val classifier = superType.qualifiers.firstNotNullOfOrNull { qualifier ->
                (qualifier as? KtClassTypeQualifier.KtResolvedClassTypeQualifier)?.symbol
            }

            if (classifier is KtClassOrObjectSymbol && classifier.classKind.isClass) return@find classifier
        }

        null
    }
}