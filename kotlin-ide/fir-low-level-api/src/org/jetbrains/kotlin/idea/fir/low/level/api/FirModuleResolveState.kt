/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirElementBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.PsiToFirCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

abstract class FirModuleResolveState {
    abstract val moduleInfo: IdeaModuleInfo
    abstract val firIdeSourcesSession: FirSession
    abstract val firIdeLibrariesSession: FirSession

    abstract fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession

    abstract fun getOrBuildFirFor(element: KtElement, toPhase: FirResolvePhase): FirElement

    abstract fun getDiagnostics(element: KtElement): List<Diagnostic>

    // todo temporary, used only in completion
    abstract fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile)

    // todo temporary, used only in completion
    abstract fun getCachedMappingForCompletion(element: KtElement): FirElement?

    abstract fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D

    // todo temporary, used only in completion
    internal abstract fun lazyResolveFunctionForCompletion(
        firFunction: FirFunction<*>,
        containerFirFile: FirFile,
        firIdeProvider: FirIdeProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector
    )

}


internal open class FirModuleResolveStateImpl(
    override val moduleInfo: IdeaModuleInfo,
    override val firIdeSourcesSession: FirSession,
    override val firIdeLibrariesSession: FirSession,
    private val sessionProvider: FirIdeSessionProvider,
    val firFileBuilder: FirFileBuilder,
    val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    val fileCache: ModuleFileCache,
) : FirModuleResolveState() {
    val psiToFirCache = PsiToFirCache(fileCache)
    val elementBuilder = FirElementBuilder(firFileBuilder, firLazyDeclarationResolver)
    private val diagnosticsCollector = DiagnosticsCollector(firFileBuilder, fileCache)

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        sessionProvider.getSession(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement, toPhase: FirResolvePhase): FirElement =
        elementBuilder.getOrBuildFirFor(element, fileCache, psiToFirCache, toPhase)

    override fun getDiagnostics(element: KtElement): List<Diagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element)

    override fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        psiToFirCache.recordElementsForCompletionFrom(fir, firFile, ktFile)
    }

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        firLazyDeclarationResolver.lazyResolveDeclaration(declaration, fileCache, toPhase, checkPCE = true)
        return declaration
    }

    override fun getCachedMappingForCompletion(element: KtElement): FirElement? =
        psiToFirCache.getCachedMapping(element)

    override fun lazyResolveFunctionForCompletion(
        firFunction: FirFunction<*>,
        containerFirFile: FirFile,
        firIdeProvider: FirIdeProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector
    ) {
        firLazyDeclarationResolver.runLazyResolveWithoutLock(
            firFunction,
            fileCache,
            containerFirFile,
            firIdeProvider,
            toPhase,
            towerDataContextCollector,
            checkPCE = false
        )
    }
}

internal fun KtElement.firResolveState(): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project).getResolveState(getModuleInfo())

