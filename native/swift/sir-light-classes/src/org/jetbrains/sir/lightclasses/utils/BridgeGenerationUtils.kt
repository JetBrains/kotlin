/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.mangler.mangledNameOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

// todo: should be changed with correct mangling KT-64970
internal val FqName.baseBridgeName: String
    get() = asString().let {
        if (it.indexOf('.') == -1) "__root___$it"
        else it.replace('.', '_')
    }

internal val <T : KaCallableSymbol> SirFromKtSymbol<T>.bridgeFqName
    get() = ktSymbol.callableId?.asSingleFqName()

internal val SirCallable.selfType: SirType? get() = when (val parent = this.parent) {
        is SirScopeDefiningDeclaration -> SirNominalType(parent)
        is SirVariable -> (parent.parent as? SirScopeDefiningDeclaration)?.let(::SirNominalType)
        is SirExtension -> parent.extendedType
        else -> null
    }

internal interface SirBridgedCallable {
    val bridges: List<SirBridge>
    var body: SirFunctionBody?
}

internal val SirScopeDefiningDeclaration.objcClassSymbolName
    get() = attributes.firstIsInstanceOrNull<SirAttribute.ObjC>()?.name
        ?: this.mangledNameOrNull
        ?: error("Failed to mangle name for briding $this")
