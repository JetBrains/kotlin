/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId


// FIXME: this has to be shared (copied from plugin example)
@OptIn(SymbolInternals::class)
fun FirDeclarationGenerationExtension.buildConstructor(classId: ClassId, isInner: Boolean, key: GeneratedDeclarationKey): FirConstructor {
    val lookupTag = ConeClassLikeLookupTagImpl(classId)
    return buildPrimaryConstructor {
        moduleData = session.moduleData
        origin = key.origin
        returnTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                lookupTag,
                emptyArray(),
                isNullable = false
            )
        }
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            EffectiveVisibility.Public
        )
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

// FIXME: copied from Parcelize plugin
inline fun FirSession.createFunction(
    owner: FirRegularClassSymbol,
    callableId: CallableId,
    init: FirSimpleFunctionBuilder.() -> Unit
): FirNamedFunctionSymbol {
    val function = buildSimpleFunction {
        moduleData = this@createFunction.moduleData
        origin = SerializationPluginKey.origin
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            if (owner.modality == Modality.FINAL) Modality.FINAL else Modality.OPEN,
            EffectiveVisibility.Public
        ).apply {
            isOverride = true
        }
        name = callableId.callableName
        symbol = FirNamedFunctionSymbol(callableId)
        dispatchReceiverType = owner.defaultType()
        init()
    }
    return function.symbol
}