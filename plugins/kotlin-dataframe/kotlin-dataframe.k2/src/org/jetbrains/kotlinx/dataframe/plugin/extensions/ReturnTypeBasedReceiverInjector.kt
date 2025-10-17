package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.captureValueInAnalyze
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ReturnTypeBasedReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    companion object {
        private val SCHEMA_TYPES = setOf(
            Names.DF_CLASS_ID,
            Names.GROUP_BY_CLASS_ID,
            Names.DATA_ROW_CLASS_ID,
            Names.COLUM_GROUP_CLASS_ID,
        )
    }

    @OptIn(SymbolInternals::class)
    override fun addNewImplicitReceivers(
        functionCall: FirFunctionCall,
        sessionHolder: SessionAndScopeSessionHolder,
        containingCallableSymbol: FirBasedSymbol<*>,
    ): List<ImplicitExtensionReceiverValue> {
        val callReturnType = functionCall.resolvedType
        val typeArguments = mutableListOf<ConeTypeProjection>()
        callReturnType.forEachType {
            if (it.classId in SCHEMA_TYPES) {
                typeArguments += it.typeArguments
            }
        }
        return typeArguments
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
            .map {
                val receiverParameter = buildReceiverParameter {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                    this.symbol = FirReceiverParameterSymbol()
                    containingDeclarationSymbol = containingCallableSymbol
                    typeRef = buildResolvedTypeRef {
                        coneType = it
                    }
                }
                receiverParameter.captureValueInAnalyze = false
                ImplicitExtensionReceiverValue(
                    receiverParameter.symbol,
                    it,
                    sessionHolder.session,
                    sessionHolder.scopeSession
                )
            }
    }
}
