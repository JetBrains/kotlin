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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.utils.addToStdlib.plusIfNotNull
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ReturnTypeBasedReceiverInjector(session: FirSession) : FirExpressionResolutionExtension(session) {
    companion object {
        private val SCHEMA_TYPES = setOf(
            Names.DF_CLASS_ID,
            Names.GROUP_BY_CLASS_ID,
            Names.DATA_ROW_CLASS_ID,
            Names.COLUM_GROUP_CLASS_ID,
            Names.FRAME_COLUMN_CLASS_ID,
        )
    }

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
        val scopeReceivers = typeArguments
            .mapNotNull {
                val symbol = (it as? ConeKotlinType)?.toRegularClassSymbol(session)
                symbol?.takeIf { it.callShapeData != null }
            }
            .takeIf { it.size == typeArguments.size }
            .orEmpty()
            .flatMap { marker ->
                marker.declaredMemberScope(session, FirResolvePhase.DECLARATIONS).collectAllProperties()
                    .filterIsInstance<FirPropertySymbol>()
                    .filter { it.getAnnotationByClassId(Names.SCOPE_PROPERTY_ANNOTATION, session) != null }
                    .map { it.resolvedReturnType }
            }
            .map { scopeType ->
                val receiverParameter = buildReceiverParameter {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                    this.symbol = FirReceiverParameterSymbol()
                    containingDeclarationSymbol = containingCallableSymbol
                    typeRef = buildResolvedTypeRef {
                        coneType = scopeType
                    }
                }
                receiverParameter.captureValueInAnalyze = false
                ImplicitExtensionReceiverValue(
                    boundSymbol = receiverParameter.symbol,
                    type = scopeType,
                    useSiteSession = sessionHolder.session,
                    scopeSession = sessionHolder.scopeSession
                )
            }
        if (scopeReceivers.isEmpty()) return scopeReceivers

        // Injected scope receivers land on top of the implicit receiver stack and would make an explicit `this`
        // resolve to a scope instead of the enclosing extension receiver (e.g. `this@run`).
        // To keep `this` pointing at the enclosing receiver, push a value bound to that very receiver parameter back on top.
        // Column accessors of the scopes are still reachable through tower resolution, so `this.select("a").a` keeps working.
        val outerReceiver = (containingCallableSymbol as? FirCallableSymbol<*>)?.let { callable ->
            val receiverParameter = callable.receiverParameterSymbol ?: return@let null
            val receiverType = callable.resolvedReceiverType ?: return@let null
            ImplicitExtensionReceiverValue(
                boundSymbol = receiverParameter,
                type = receiverType,
                useSiteSession = sessionHolder.session,
                scopeSession = sessionHolder.scopeSession
            )
        }

        return scopeReceivers.plusIfNotNull(outerReceiver)
    }
}
