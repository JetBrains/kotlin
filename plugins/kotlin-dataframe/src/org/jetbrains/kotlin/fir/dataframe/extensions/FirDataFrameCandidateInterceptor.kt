package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameterCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.extensions.FirCandidateFactoryInterceptor
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRefCopy
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.math.abs

class FirDataFrameCandidateInterceptor(
    session: FirSession,
    val callableNames: ArrayDeque<CallableId>,
    val callableState: MutableMap<Name, FirSimpleFunction>,
    val nextName: (String?) -> ClassId
) : FirCandidateFactoryInterceptor(session) {
    val Key = FirDataFrameExtensionsGenerator.DataFramePlugin

    @OptIn(SymbolInternals::class)
    override fun intercept(callInfo: CallInfo, symbol: FirBasedSymbol<*>, dispatchReceiverValue: ReceiverValue?): FirBasedSymbol<*> {
        val callSiteAnnotations = (callInfo.callSite as? FirAnnotationContainer)?.annotations ?: emptyList()
        if (callSiteAnnotations.any { it.fqName(session)?.shortName()?.equals(Name.identifier("DisableInterpretation")) == true }) {
            return symbol
        }
        if (symbol.annotations.none { it.fqName(session)?.shortName()?.equals(Name.identifier("Refine")) == true }) {
            return symbol
        }
        if (symbol !is FirNamedFunctionSymbol) return symbol
        val lookupTag = ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID)
        val generatedName = callableNames.removeLast()
        val newSymbol = FirNamedFunctionSymbol(generatedName)
        var hash = callInfo.name.hashCode() + callInfo.arguments.sumOf {
            when (it) {
                is FirConstExpression<*> -> it.value.hashCode()
                else -> 42
            }
        }
        hash = abs(hash)
        // possibly null if explicit receiver type is AnyFrame
        val argument = (callInfo.explicitReceiver?.typeRef as? FirResolvedTypeRef)?.type?.typeArguments?.singleOrNull()
        val suggestedName = if (argument == null) {
            "${callInfo.name.identifier.titleCase()}_$hash"
        } else {
            when (argument) {
                is ConeStarProjection -> "${callInfo.name.identifier.titleCase()}_$hash"
                is ConeKotlinTypeProjection -> {
                    val titleCase = argument.type.classId?.shortClassName?.identifier?.titleCase()?.substringBeforeLast("_")
                    "${titleCase}_$hash"
                }
            }
        }
        val tokenId = nextName(suggestedName)
        val typeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                lookupTag,
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(tokenId),
                        emptyArray(),
                        isNullable = false
                    )
                ),
                isNullable = false
            )
        }

        val function = buildSimpleFunctionCopy(symbol.fir) {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(Key)
            source = null
            containerSource = null
            val substitutorMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
            typeParameters.replaceAll {
                val originalSymbol = it.symbol

                val newTypeParameterSymbol = FirTypeParameterSymbol()

                substitutorMap[originalSymbol] = ConeTypeParameterTypeImpl(
                    ConeTypeParameterLookupTag(newTypeParameterSymbol),
                    isNullable = false
                )

                buildTypeParameterCopy(it) {
                    moduleData = session.moduleData
                    source = null
                    origin = FirDeclarationOrigin.Plugin(Key)
                    this.symbol = newTypeParameterSymbol
                    containingDeclarationSymbol = newSymbol
                }.also { newTypeParameterSymbol.bind(it) }
            }
            val substitutorByMap = substitutorByMap(substitutorMap, session)

            receiverParameter = buildReceiverParameterCopy(receiverParameter!!) {
                this.typeRef = buildResolvedTypeRefCopy(this.typeRef as FirResolvedTypeRef) {
                    type = substitutorByMap.substituteOrSelf(type)
                }
            }

            valueParameters.replaceAll {
                buildValueParameterCopy(it) {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(Key)
                    val myReturnTypeRef = returnTypeRef
                    if (myReturnTypeRef is FirResolvedTypeRef) {
                        returnTypeRef = buildResolvedTypeRefCopy(myReturnTypeRef) {
                            type = substitutorByMap.substituteOrSelf(type)
                        }
                    }
                }
            }

            name = generatedName.callableName
            body = null
            this.symbol = newSymbol
            returnTypeRef = typeRef
        }.also { newSymbol.bind(it) }
        callableState[generatedName.callableName] = function
        return newSymbol
    }
}
