/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.extensions

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.sir.SirCallableKind

internal val KaCallableSymbol.sirCallableKind: SirCallableKind
    get() = when (symbolKind) {
        KaSymbolKind.TOP_LEVEL -> {
            val isRootPackage = callableId?.packageName?.isRoot
            if (isRootPackage == true) {
                SirCallableKind.FUNCTION
            } else {
                SirCallableKind.STATIC_METHOD
            }
        }
        KaSymbolKind.CLASS_MEMBER, KaSymbolKind.ACCESSOR,
        -> SirCallableKind.INSTANCE_METHOD
        KaSymbolKind.LOCAL,
        KaSymbolKind.SAM_CONSTRUCTOR,
        -> TODO("encountered callable kind($symbolKind) that is not translatable currently. Fix this crash during KT-65980.")
    }

internal fun KaSymbol.documentation(): String? = this.psiSafe<KtDeclaration>()?.docComment?.text
