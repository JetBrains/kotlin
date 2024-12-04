/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * Injects Algebra<T> as implicit receiver if `injectAlgebra<T>()` was called
 */
class AlgebraReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    companion object {
        private val INJECT_ALGEBRA_NAME = Name.identifier("injectAlgebra")
        private val ALGEBRA_CLASS_ID = ClassId.topLevel(FqName.topLevel(Name.identifier("Algebra")))
    }

    private object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "AlgebraReceiverGeneratorKey"
        }
    }

    private val algebraOrigin = FirDeclarationOrigin.Plugin(Key)

    override fun addNewImplicitReceivers(
        functionCall: FirFunctionCall,
        sessionHolder: SessionHolder,
        containingCallableSymbol: FirCallableSymbol<*>,
    ): List<ImplicitExtensionReceiverValue> {
        if (functionCall.calleeReference.name != INJECT_ALGEBRA_NAME) return emptyList()
        val typeProjection = functionCall.typeArguments.firstOrNull() as? FirTypeProjectionWithVariance ?: return emptyList()
        val argumentType = typeProjection.typeRef.coneType
        val algebraType = ALGEBRA_CLASS_ID.createConeType(session, arrayOf(argumentType))
        val receiverParameter = buildReceiverParameter {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = algebraOrigin
            symbol = FirReceiverParameterSymbol()
            containingDeclarationSymbol = containingCallableSymbol
            typeRef = buildResolvedTypeRef {
                coneType = algebraType
            }
        }
        val extensionReceiverValue = ImplicitExtensionReceiverValue(
            receiverParameter.symbol,
            algebraType,
            sessionHolder.session,
            sessionHolder.scopeSession
        )
        return listOf(extensionReceiverValue)
    }
}
