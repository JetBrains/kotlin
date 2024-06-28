/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

internal fun KaSession.getImplementsCloneable(symbol: KaDeclarationSymbol?): Boolean {

    val classSymbol = symbol as? KaClassSymbol
    return if (classSymbol != null) {
        getImplementsCloneable(classSymbol)
    } else {
        false
    }
}

internal fun KaSession.getImplementsCloneable(symbol: KaClassSymbol): Boolean {
    return symbol.superTypes.any {
        it.expandedSymbol?.isCloneable ?: false
    }
}

internal val KaClassSymbol.isCloneable: Boolean
    get() {
        return classId?.isCloneable ?: false
    }

internal val ClassId.isCloneable: Boolean
    get() {
        return asSingleFqName() == StandardNames.FqNames.cloneable.toSafe()
    }

internal fun KaSession.isClone(symbol: KaNamedFunctionSymbol): Boolean {
    val cloneCallableId = CallableId(StandardClassIds.Cloneable, Name.identifier("clone"))
    if (symbol.callableId == cloneCallableId) return true

    return symbol.allOverriddenSymbols.any { it.callableId == cloneCallableId }
}
