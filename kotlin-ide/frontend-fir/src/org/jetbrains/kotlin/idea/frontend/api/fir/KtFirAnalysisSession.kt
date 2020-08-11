/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.*
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper.toTargetSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KtFirAnalysisSession
private constructor(
    private val element: KtElement,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    val isContextSession: Boolean,
) : KtAnalysisSession(token) {
    private val project = element.project

    override val symbolProvider: KtSymbolProvider =
        KtFirSymbolProvider(
            token,
            firResolveState.firIdeLibrariesSession.firSymbolProvider,
            firResolveState,
            firSymbolBuilder
        )

    private val firScopeStorage = FirScopeRegistry()

    override val scopeProvider: KtScopeProvider by threadLocal {
        KtFirScopeProvider(token, firSymbolBuilder, project, firResolveState, firScopeStorage)
    }

    init {
        assertIsValid()
    }

    override fun getSmartCastedToTypes(expression: KtExpression): Collection<KtType>? = withValidityAssertion {
        // TODO filter out not used smartcasts
        expression.getOrBuildFirSafe<FirExpressionWithSmartcast>(firResolveState)?.typesFromSmartCast?.map { it.asTypeInfo() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast> = withValidityAssertion {
        // TODO filter out not used smartcasts
        val qualifiedExpression = expression.getOrBuildFirSafe<FirQualifiedAccessExpression>(firResolveState) ?: return emptyList()
        if (qualifiedExpression.dispatchReceiver !is FirExpressionWithSmartcast
            && qualifiedExpression.extensionReceiver !is FirExpressionWithSmartcast
        ) return emptyList()
        buildList {
            (qualifiedExpression.dispatchReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asTypeInfo() },
                    ImplicitReceiverSmartcastKind.DISPATCH
                )
            }?.let(::add)
            (qualifiedExpression.extensionReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asTypeInfo() },
                    ImplicitReceiverSmartcastKind.EXTENSION
                )
            }?.let(::add)
        }
    }


    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration<*>>(firResolveState)
        firDeclaration.returnTypeRef.coneType.asTypeInfo()
    }

    override fun getKtExpressionType(expression: KtExpression): KtType = withValidityAssertion {
        expression.getOrBuildFirOfType<FirExpression>(firResolveState).typeRef.coneType.asTypeInfo()
    }

    override fun isSubclassOf(klass: KtClassOrObject, superClassId: ClassId): Boolean {
        assertIsValid()
        var result = false
        forEachSuperClass(klass.getOrBuildFirSafe(firResolveState) ?: return false) { type ->
            result = result || type.firClassLike(firResolveState.firIdeSourcesSession)?.symbol?.classId == superClassId
        }
        return result
    }

    override fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic> = withValidityAssertion {
        LowLevelFirApiFacade.getDiagnosticsFor(element, firResolveState)
    }

    override fun resolveCall(call: KtBinaryExpression): CallInfo? = withValidityAssertion {
        val firCall = call.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null
        resolveCall(firCall, call)
    }

    override fun createContextDependentCopy(): KtAnalysisSession {
        check(!isContextSession) { "Cannot create context-dependent copy of KtAnalysis session from a context dependent one" }
        val contextResolveState = LowLevelFirApiFacade.getResolveStateForCompletion(element, firResolveState)
        return KtFirAnalysisSession(
            element,
            contextResolveState,
            firSymbolBuilder.createReadOnlyCopy(contextResolveState),
            token,
            isContextSession = true
        )
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
        val session = firResolveState.firIdeSourcesSession
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

    private fun forEachSuperClass(firClass: FirClass<*>, action: (FirResolvedTypeRef) -> Unit) {
        firClass.superTypeRefs.forEach { superType ->
            (superType as? FirResolvedTypeRef)?.let(action)
            (superType.firClassLike(firClass.session) as? FirClass<*>?)?.let { forEachSuperClass(it, action) }
        }
    }

    private fun ConeKotlinType.asTypeInfo() = firSymbolBuilder.buildKtType(this)

    companion object {
        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(KotlinBuiltIns.getFunctionClassId(arity), Name.identifier("invoke")),
                CallableId(KotlinBuiltIns.getSuspendFunctionClassId(arity), Name.identifier("invoke"))
            )
        }

        @Deprecated("Please use org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProviderKt.analyze")
        internal fun createForElement(element: KtElement): KtFirAnalysisSession {
            val firResolveState = LowLevelFirApiFacade.getResolveStateFor(element)
            val project = element.project
            val token = ReadActionConfinementValidityToken(project)
            val firSymbolBuilder = KtSymbolByFirBuilder(
                firResolveState,
                project,
                token
            )
            return KtFirAnalysisSession(
                element,
                firResolveState,
                firSymbolBuilder,
                token,
                isContextSession = false
            )
        }
    }
}

/**
 * Stores strong references to all instances of [FirScope] used
 * Needed as the only entity which may have a strong references to FIR internals is [KtFirAnalysisSession]
 * Entities which needs storing [FirScope] instances will store them as weak references via [org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef]
 */
internal class FirScopeRegistry {
    private val scopes = mutableListOf<FirScope>()

    fun register(scope: FirScope) {
        scopes += scope
    }
}