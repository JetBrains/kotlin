/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.mangler.mangledNameOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

internal val List<String>.forBridge: List<String>
    get() = this.singleOrNull()?.let { listOf("__root__", it) } ?: this // todo: should be changed with correct mangling KT-64970

internal val <T: KaCallableSymbol> SirFromKtSymbol<T>.bridgeFqName get() = ktSymbol
        .callableId?.asSingleFqName()
        ?.pathSegments()?.map { it.toString() }

internal val SirCallable.selfType: SirType? get() = when (val parent = this.parent) {
        is SirNamedDeclaration -> SirNominalType(parent as SirNamedDeclaration)
        is SirVariable -> (parent.parent as? SirNamedDeclaration)?.let(::SirNominalType)
        is SirExtension -> parent.extendedType
        else -> null
    }

internal interface SirBridgedCallable {
    val bridges: List<SirBridge>
    var body: SirFunctionBody?
}

internal val SirNamedDeclaration.objcClassSymbolName
    get() = attributes.firstIsInstanceOrNull<SirAttribute.ObjC>()?.name
        ?: this.mangledNameOrNull
        ?: error("Failed to mangle name for briding $this")
