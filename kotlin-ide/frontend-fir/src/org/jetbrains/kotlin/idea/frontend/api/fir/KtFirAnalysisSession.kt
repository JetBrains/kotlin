/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ReadActionConfinementValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.assertIsValid
import org.jetbrains.kotlin.idea.frontend.api.components.*
import org.jetbrains.kotlin.idea.frontend.api.fir.components.*
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.ResolutionParameters
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.threadLocal
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class KtFirAnalysisSession
private constructor(
    private val element: KtElement,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    val isContextSession: Boolean,
) : KtAnalysisSession(token) {
    init {
        assertIsValid()
    }

    private val project = element.project

    override val smartCastProvider: KtSmartCastProvider = KtFirSmartcastProvider(this, token)
    override val typeProvider: KtTypeProvider = KtFirTypeProvider(this, token)
    override val diagnosticProvider: KtDiagnosticProvider = KtFirDiagnosticProvider(this, token)
    override val containingDeclarationProvider = KtFirSymbolContainingDeclarationProvider(this, token)
    override val callResolver: KtCallResolver = KtFirCallResolver(this, token)
    override val scopeProvider by threadLocal { KtFirScopeProvider(this, firSymbolBuilder, project, firResolveState, token) }
    override val symbolProvider: KtSymbolProvider =
        KtFirSymbolProvider(this, firResolveState.firIdeLibrariesSession.firSymbolProvider, firResolveState, firSymbolBuilder, token)


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

    private val completionContextCache = HashMap<Pair<FirFile, KtNamedFunction>, LowLevelFirApiFacade.FirCompletionContext>()

    // Temporary kludge to check single candidate resolver in tests
    override fun resolveAndCheckReceivers(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleReceiver: KtExpression?,
    ): Boolean {
        firSymbolForCandidate.safeAs<KtFirFunctionSymbol>()?.firRef?.withFir { firFunction ->
            val file = originalFile.getOrBuildFirOfType<FirFile>(firResolveState)
            val explicitReceiverExpression = possibleReceiver?.getOrBuildFirOfType<FirExpression>(firResolveState)
            val resolver = SingleCandidateResolver(firResolveState.firIdeSourcesSession, file)
            val enclosingFunction = nameExpression.getNonStrictParentOfType<KtNamedFunction>()
                ?: error("Cannot find enclosing function for ${nameExpression.getElementTextInContext()}")

            val completionContext = completionContextCache.getOrCreate(file to enclosingFunction) {
                LowLevelFirApiFacade.buildCompletionContextForFunction(
                    file,
                    enclosingFunction,
                    enclosingFunction,
                    state = firResolveState
                )
            }

            val towerDataContext = completionContext.getTowerDataContext(nameExpression)

            val implicitReceivers = sequence {
                yield(null)
                yieldAll(towerDataContext.implicitReceiverStack)
            }
            for (implicitReceiverValue in implicitReceivers) {
                val resolutionParameters = ResolutionParameters(
                    singleCandidateResolutionMode = SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION,
                    callableSymbol = firFunction.symbol,
                    implicitReceiver = implicitReceiverValue,
                    explicitReceiver = explicitReceiverExpression
                )
                resolver.resolveSingleCandidate(resolutionParameters)?.let {
                    // not null if resolved and completed successfully
                    return true
                }
            }
        }
        return false
    }

    companion object {
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

