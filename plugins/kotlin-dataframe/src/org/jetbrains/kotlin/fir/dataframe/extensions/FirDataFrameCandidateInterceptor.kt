package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.extensions.FirCandidateFactoryInterceptor
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirDataFrameCandidateInterceptor(
    session: FirSession,
    val callableNames: ArrayDeque<CallableId>,
    val tokenIds: ArrayDeque<ClassId>,
    val callableState: MutableMap<Name, FirSimpleFunction>
) : FirCandidateFactoryInterceptor(session) {
    val Key = FirDataFrameExtensionsGenerator.DataFramePlugin

    @OptIn(SymbolInternals::class)
    override fun intercept(symbol: FirBasedSymbol<*>): FirBasedSymbol<*> {
        if (symbol.annotations.none { it.fqName(session)?.shortName()?.equals(Name.identifier("Refine")) == true }) {
            return symbol
        }
        if (symbol !is FirNamedFunctionSymbol) return symbol
        val lookupTag = ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID)
        val generatedName = callableNames.removeLast()
        val newSymbol = FirNamedFunctionSymbol(generatedName)
        val tokenId = tokenIds.removeLast()
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
            source = null
            typeParameters.replaceAll {
                buildTypeParameterCopy(it) {
                    containingDeclarationSymbol = newSymbol
                }
            }
            name = generatedName.callableName
            body = null
            origin = FirDeclarationOrigin.Plugin(Key)
            this.symbol = newSymbol
            returnTypeRef = typeRef
        }.also { newSymbol.bind(it) }
        callableState[generatedName.callableName] = function
        return newSymbol
    }
}
