/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class DataFrameLikeReturnTypeInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    companion object {
        val DF_CLASS_ID: ClassId = ClassId.topLevel(FqName.fromSegments(listOf("DataFrame")))
    }

    private object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "DataFrameLikeReturnTypeInjectorGeneratorKey"
        }
    }

    private val dataFrameLikeOrigin = FirDeclarationOrigin.Plugin(Key)

    @OptIn(SymbolInternals::class)
    override fun addNewImplicitReceivers(
        functionCall: FirFunctionCall,
        sessionHolder: SessionHolder,
        containingCallableSymbol: FirCallableSymbol<*>,
    ): List<ImplicitExtensionReceiverValue> {
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
            .map {
                val receiverParameter = buildReceiverParameter {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    moduleData = session.moduleData
                    origin = dataFrameLikeOrigin
                    this.symbol = FirReceiverParameterSymbol()
                    containingDeclarationSymbol = containingCallableSymbol
                    typeRef = buildResolvedTypeRef {
                        coneType = it.resolvedReturnType
                    }
                }
                ImplicitExtensionReceiverValue(
                    receiverParameter.symbol,
                    it.resolvedReturnType,
                    sessionHolder.session,
                    sessionHolder.scopeSession
                )
            }
    }
}
