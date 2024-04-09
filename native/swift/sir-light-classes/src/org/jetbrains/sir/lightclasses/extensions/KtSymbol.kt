/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.extensions

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.sir.SirCallableKind

internal val KtCallableSymbol.sirCallableKind: SirCallableKind
    get() = when (symbolKind) {
        KtSymbolKind.TOP_LEVEL -> {
            val isRootPackage = callableIdIfNonLocal?.packageName?.isRoot
            if (isRootPackage == true) {
                SirCallableKind.FUNCTION
            } else {
                SirCallableKind.STATIC_METHOD
            }
        }
        KtSymbolKind.CLASS_MEMBER, KtSymbolKind.ACCESSOR,
        -> SirCallableKind.INSTANCE_METHOD
        KtSymbolKind.LOCAL,
        KtSymbolKind.SAM_CONSTRUCTOR,
        -> TODO("encountered callable kind($symbolKind) that is not translatable currently. Fix this crash during KT-65980.")
    }

internal fun KtSymbol.documentation(): String? = this.psiSafe<KtDeclaration>()?.docComment?.text
