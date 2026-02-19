/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.extensions

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.sir.SirCallableKind
import org.jetbrains.kotlin.sir.SirModality

internal val KaCallableSymbol.sirCallableKind: SirCallableKind
    get() = when (location) {
        KaSymbolLocation.TOP_LEVEL -> {
            val isRootPackage = callableId?.packageName?.isRoot
            if (isRootPackage == true) {
                SirCallableKind.FUNCTION
            } else {
                SirCallableKind.CLASS_METHOD
            }
        }
        KaSymbolLocation.CLASS, KaSymbolLocation.PROPERTY,
            -> SirCallableKind.INSTANCE_METHOD
        KaSymbolLocation.LOCAL,
            -> TODO("encountered callable location($location) that is not translatable currently. Fix this crash during KT-65980.")
    }

internal fun KaSymbol.documentation(): String? = this.psiSafe<KtDeclaration>()?.docComment?.text

internal val KaSymbolModality.sirModality: SirModality
    get() = when (this) {
        KaSymbolModality.FINAL -> SirModality.FINAL
        KaSymbolModality.SEALED -> SirModality.UNSPECIFIED
        KaSymbolModality.OPEN, KaSymbolModality.ABSTRACT -> SirModality.OPEN
    }