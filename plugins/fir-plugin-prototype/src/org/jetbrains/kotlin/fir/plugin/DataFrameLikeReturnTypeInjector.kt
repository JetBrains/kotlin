/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class DataFrameLikeReturnTypeInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    companion object {
        val DF_CLASS_ID: ClassId = ClassId.topLevel(FqName.fromSegments(listOf("DataFrame")))
    }

    @OptIn(SymbolInternals::class)
    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val callReturnType = functionCall.resolvedType
        if (callReturnType.classId != DF_CLASS_ID) return emptyList()
        val rootMarker = callReturnType.typeArguments[0]
        if (rootMarker !is ConeClassLikeType) {
            return emptyList()
        }
        val symbol = rootMarker.toRegularClassSymbol(session) ?: return emptyList()
        return symbol.declaredMemberScope(session, FirResolvePhase.DECLARATIONS).collectAllProperties()
            .filterIsInstance<FirPropertySymbol>()
            .filter {
                val data = it.resolvedReturnType.toRegularClassSymbol(session)?.fir?.callShapeData ?: return@filter false
                data is CallShapeData.Scope
            }
            .map { it.resolvedReturnType }
    }
}