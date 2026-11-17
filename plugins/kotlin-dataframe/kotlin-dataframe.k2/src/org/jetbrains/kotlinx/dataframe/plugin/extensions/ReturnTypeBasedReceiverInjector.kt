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
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
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
        // detect whether there exists an outer `this` and save it so we can intersect our new scopeType with it
        val outerReceiver = (containingCallableSymbol as? FirCallableSymbol<*>)?.let { callable ->
            val receiverParameter = callable.receiverParameterSymbol ?: return@let null
            val receiverType = callable.resolvedReceiverType ?: return@let null
            Pair(receiverParameter, receiverType)
        }
        val typeArguments = mutableListOf<ConeTypeProjection>()
        callReturnType.forEachType {
            if (it.classId in SCHEMA_TYPES) {
                typeArguments += it.typeArguments
            }
        }
        return typeArguments
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
                val receiverSymbol: FirReceiverParameterSymbol
                val receiverType: ConeKotlinType

                if (outerReceiver != null) {
                    // if we have an outer `this` receiver, intersect our new scopeType with its type so calling `this` remains the same
                    val [outerParameter, outerType] = outerReceiver
                    receiverSymbol = outerParameter
                    receiverType = ConeTypeIntersector.intersectTypes(session.typeContext, listOf(outerType, scopeType))
                } else {
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
                    receiverSymbol = receiverParameter.symbol
                    receiverType = scopeType
                }

                ImplicitExtensionReceiverValue(
                    boundSymbol = receiverSymbol,
                    type = receiverType,
                    useSiteSession = sessionHolder.session,
                    scopeSession = sessionHolder.scopeSession
                )
            }
    }
}
