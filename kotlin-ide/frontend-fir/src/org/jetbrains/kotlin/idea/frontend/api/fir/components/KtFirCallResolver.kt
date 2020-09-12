/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.declarations.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.fir.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.isImplicitFunctionCall
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.components.KtCallResolver
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper.toTargetSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirCallResolver(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCallResolver(), KtFirAnalysisSessionComponent {

    override fun resolveCall(call: KtBinaryExpression): CallInfo? = withValidityAssertion {
        val firCall = call.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null
        resolveCall(firCall, call)
    }

    override fun resolveCall(call: KtCallExpression): CallInfo? = withValidityAssertion {
        val firCall = when (val fir = call.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> fir
            is FirSafeCallExpression -> fir.regularQualifiedAccess as? FirFunctionCall
            else -> null
        } ?: return null
        return resolveCall(firCall, call)
    }

    private fun resolveCall(firCall: FirFunctionCall, callExpression: KtExpression): CallInfo? {
        val session = firResolveState.rootModuleSession
        val resolvedFunctionSymbol = firCall.calleeReference.toTargetSymbol(session, firSymbolBuilder)
        val resolvedCalleeSymbol = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        return when {
            resolvedCalleeSymbol is FirConstructorSymbol -> {
                val fir = resolvedCalleeSymbol.fir
                FunctionCallInfo(firSymbolBuilder.buildConstructorSymbol(fir))
            }
            firCall.dispatchReceiver is FirQualifiedAccessExpression && firCall.isImplicitFunctionCall() -> {
                val target = with(FirReferenceResolveHelper) {
                    val calleeReference = (firCall.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
                    calleeReference.toTargetSymbol(session, firSymbolBuilder)
                }
                when (target) {
                    null -> null
                    is KtVariableLikeSymbol -> {
                        val functionSymbol =
                            (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
                        when (functionSymbol?.callableId) {
                            null -> null
                            in kotlinFunctionInvokeCallableIds -> VariableAsFunctionCallInfo(target, functionSymbol.fir.isSuspend)
                            else -> (resolvedFunctionSymbol as? KtFunctionSymbol)
                                ?.let { VariableAsFunctionLikeCallInfo(target, it) }
                        }
                    }
                    else -> resolvedFunctionSymbol?.asSimpleFunctionCall()
                }
            }
            else -> resolvedFunctionSymbol?.asSimpleFunctionCall()
        }
    }

    private fun KtSymbol.asSimpleFunctionCall() =
        (this as? KtFunctionSymbol)?.let(::FunctionCallInfo)

    companion object {
        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(StandardNames.getFunctionClassId(arity), Name.identifier("invoke")),
                CallableId(StandardNames.getSuspendFunctionClassId(arity), Name.identifier("invoke"))
            )
        }
    }
}
