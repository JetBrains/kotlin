package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.toSymbol

class ReturnTypeBasedReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val token = generatedTokenOrNull(functionCall) ?: return emptyList()
        val symbol = token.toSymbol(session)!! as FirRegularClassSymbol
        return symbol.declarationSymbols.filterIsInstance<FirPropertySymbol>().map { it.resolvedReturnType }
    }

    private fun generatedTokenOrNull(call: FirFunctionCall): ConeClassLikeType? {
        val callReturnType = call.typeRef.coneTypeSafe<ConeClassLikeType>() ?: return null
        if (callReturnType.classId != Names.DF_CLASS_ID) return null
        val rootMarker = callReturnType.typeArguments[0]
        if (rootMarker !is ConeClassLikeType) {
            return null
        }
        return rootMarker.takeIf { it.classId?.shortClassName?.asString()?.startsWith("Token") == true }
    }
}
