/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.containingKtFileIfAny
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class FirModuleResolveStateForCompletion(
    override val project: Project,
    private val originalState: FirModuleResolveStateImpl
) : FirModuleResolveState() {
    override val moduleInfo: IdeaModuleInfo get() = originalState.moduleInfo

    override val rootModuleSession get() = originalState.rootModuleSession
    private val fileStructureCache = originalState.fileStructureCache

    private val completionMapping = mutableMapOf<KtElement, FirElement>()

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement): FirElement {
        val psi = originalState.elementBuilder.getPsiAsFirElementSource(element)
        synchronized(completionMapping) { completionMapping[psi] }?.let { return it }
        return originalState.elementBuilder.getOrBuildFirFor(
            element,
            originalState.firFileBuilder,
            originalState.rootModuleSession.cache,
            fileStructureCache,
        )
    }

    override fun getFirFile(ktFile: KtFile): FirFile =
        originalState.getFirFile(ktFile)

    override fun isFirFileBuilt(ktFile: KtFile): Boolean {
        error("Should not be called in in completion")
    }

    override fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        synchronized(completionMapping) { fir.accept(FirElementsRecorder(), completionMapping) }
    }

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        return originalState.resolvedFirToPhase(declaration, toPhase)
    }

    override fun lazyResolveDeclarationForCompletion(
        firFunction: FirDeclaration,
        containerFirFile: FirFile,
        firIdeProvider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector
    ) {
        originalState.lazyResolveDeclarationForCompletion(firFunction, containerFirFile, firIdeProvider, toPhase, towerDataContextCollector)
    }

    override fun getFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? {
        val ktFile = declaration.containingKtFileIfAny ?: return null
        cache.getCachedFirFile(ktFile)?.let { return it }
        ktFile.originalKtFile?.let(cache::getCachedFirFile)?.let { return it }
        return null
    }


    override fun getDiagnostics(element: KtElement): List<Diagnostic> {
        error("Diagnostics should not be retrieved in completion")
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic> {
        error("Diagnostics should not be retrieved in completion")
    }

    @OptIn(InternalForInline::class)
    override fun findNonLocalSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        error("Should not be used in completion")
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        error("Should not be used in completion")
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration {
        error("Should not be used in completion")
    }

    override fun getBuiltFirFileOrNull(ktFile: KtFile): FirFile? {
        error("Should not be used in completion")
    }

    override fun getTowerDataContextForElement(element: KtElement): FirTowerDataContext? =
        originalState.getTowerDataContextForElement(element)
}