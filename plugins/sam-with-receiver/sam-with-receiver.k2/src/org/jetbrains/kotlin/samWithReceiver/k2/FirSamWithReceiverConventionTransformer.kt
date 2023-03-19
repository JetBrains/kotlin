/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver.k2

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.FirSamConversionTransformerExtension
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.functionTypeService
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirSamWithReceiverConventionTransformer(
    private val annotations: List<String>,
    session: FirSession
) : FirSamConversionTransformerExtension(session) {
    override fun getCustomFunctionTypeForSamConversion(function: FirSimpleFunction): ConeLookupTagBasedType? {
        val containingClassSymbol = function.containingClassLookupTag()?.toFirRegularClassSymbol(session) ?: return null
        return runIf(containingClassSymbol.resolvedAnnotationClassIds.any { it.asSingleFqName().asString() in annotations }) {
            val parameterTypes = function.valueParameters.map { it.returnTypeRef.coneType }
            if (parameterTypes.isEmpty()) return null
            val kind = session.functionTypeService.extractSingleSpecialKindForFunction(function.symbol) ?: FunctionTypeKind.Function
            createFunctionType(
                kind,
                parameters = parameterTypes.subList(1, parameterTypes.size),
                receiverType = parameterTypes[0],
                rawReturnType = function.returnTypeRef.coneType
            )
        }
    }
}
