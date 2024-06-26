package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

class ReturnTypeBasedReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val symbol = generatedTokenOrNull(functionCall) ?: return emptyList()
        return symbol.declaredMemberScope(session, FirResolvePhase.DECLARATIONS).collectAllProperties()
            .filterIsInstance<FirPropertySymbol>()
            .filter { it.getAnnotationByClassId(Names.SCOPE_PROPERTY_ANNOTATION, session) != null }
            .map { it.resolvedReturnType }
    }

    @OptIn(SymbolInternals::class)
    private fun generatedTokenOrNull(call: FirFunctionCall): FirRegularClassSymbol? {
        val callReturnType = call.resolvedType
        if (callReturnType.classId != Names.DF_CLASS_ID) return null
        val rootMarker = callReturnType.typeArguments[0]
        if (rootMarker !is ConeClassLikeType) {
            return null
        }

        val symbol = rootMarker.toRegularClassSymbol(session)
        return symbol.takeIf { it?.fir?.callShapeData != null }
    }
}
