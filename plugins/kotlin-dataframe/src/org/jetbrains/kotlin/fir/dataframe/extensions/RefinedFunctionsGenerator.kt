package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class RefinedFunctionsGenerator(
    session: FirSession,
    val callables: Set<CallableId>,
    val callableState: MutableMap<Name, FirSimpleFunction>
) : FirDeclarationGenerationExtension(session) {
    override fun getTopLevelCallableIds(): Set<CallableId> {
        return buildSet {
            addAll(callables)
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val state = callableState[callableId.callableName] ?: return emptyList()
        return listOf(state.symbol)
    }
}
