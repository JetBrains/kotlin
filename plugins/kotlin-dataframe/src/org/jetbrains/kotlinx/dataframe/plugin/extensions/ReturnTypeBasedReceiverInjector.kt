package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ReturnTypeBasedReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    @OptIn(SymbolInternals::class)
    override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
        val callReturnType = functionCall.resolvedType
        return if (callReturnType.classId in setOf(Names.DF_CLASS_ID, Names.GROUP_BY_CLASS_ID)) {
            val typeArguments = callReturnType.typeArguments
            typeArguments
                .mapNotNull {
                    val symbol = (it as? ConeKotlinType)?.toRegularClassSymbol(session)
                    symbol?.takeIf { it.fir.callShapeData != null }
                }
                .takeIf { it.size == typeArguments.size }
                .orEmpty()
                .flatMap { marker ->
                    marker.declaredMemberScope(session, FirResolvePhase.DECLARATIONS).collectAllProperties()
                        .filterIsInstance<FirPropertySymbol>()
                        .filter { it.getAnnotationByClassId(Names.SCOPE_PROPERTY_ANNOTATION, session) != null }
                        .map { it.resolvedReturnType }
                }
        } else {
            emptyList()
        }
    }
}
