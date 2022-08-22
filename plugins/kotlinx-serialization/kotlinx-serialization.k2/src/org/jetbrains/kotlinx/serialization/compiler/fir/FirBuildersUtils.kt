/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance


// FIXME KT-53096: this has to be shared (copied from plugin example)
@OptIn(SymbolInternals::class)
fun FirDeclarationGenerationExtension.buildPrimaryConstructor(owner: FirClassSymbol<*>, isInner: Boolean, key: GeneratedDeclarationKey, status: FirDeclarationStatus): FirConstructor {
    val classId = owner.classId
    val lookupTag = ConeClassLikeLookupTagImpl(classId)
    return buildPrimaryConstructor {
        moduleData = session.moduleData
        origin = key.origin
        returnTypeRef = run {
            owner.defaultType().toFirResolvedTypeRef()
        }
        this.status = status
        symbol = FirConstructorSymbol(classId)
        if (isInner && classId.isNestedClass) {
            dispatchReceiverType = classId.parentClassId?.let {
                val firClass = session.symbolProvider.getClassLikeSymbolByClassId(it)?.fir as? FirClass
                firClass?.defaultType()
            }
        }
    }.also {
        it.containingClassForStaticMemberAttr = lookupTag
    }
}

fun newSimpleTypeParameter(firSession: FirSession, containingDeclarationSymbol: FirBasedSymbol<*>, name: Name) = buildTypeParameter {
    moduleData = firSession.moduleData
    origin = SerializationPluginKey.origin
    resolvePhase = FirResolvePhase.BODY_RESOLVE
    variance = Variance.INVARIANT
    this.name = name
    symbol = FirTypeParameterSymbol()
    this.containingDeclarationSymbol = containingDeclarationSymbol
    isReified = false
    addDefaultBoundIfNecessary()
}

fun newSimpleValueParameter(firSession: FirSession, typeRef: FirResolvedTypeRef, name: Name) = buildValueParameter {
    moduleData = firSession.moduleData
    origin = SerializationPluginKey.origin
    this.name = name
    this.symbol = FirValueParameterSymbol(this.name)
    returnTypeRef = typeRef
    isCrossinline = false
    isNoinline = false
    isVararg = false
}
