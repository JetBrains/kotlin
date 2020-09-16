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
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class FirModuleResolveStateForCompletion(
    private val originalState: FirModuleResolveStateImpl
) : FirModuleResolveState() {
    override val moduleInfo: IdeaModuleInfo get() = originalState.moduleInfo
    override val rootModuleSession get() = originalState.rootModuleSession
    override val firTransformerProvider: FirTransformerProvider get() = originalState.firTransformerProvider
    private val fileStructureCache = originalState.fileStructureCache

    private val completionMapping = mutableMapOf<KtElement, FirElement>()

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement, toPhase: FirResolvePhase): FirElement {
        completionMapping[originalState.elementBuilder.getPsiAsFirElementSource(element)]?.let { return it }
        return originalState.elementBuilder.getOrBuildFirFor(
            element,
            originalState.rootModuleSession.cache,
            fileStructureCache,
        )
    }

    override fun getFirFile(ktFile: KtFile): FirFile =
        originalState.getFirFile(ktFile)

    override fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        fir.accept(FirElementsRecorder(), completionMapping)
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

    override fun getDiagnostics(element: KtElement): List<Diagnostic> {
        error("Diagnostics should not be retrieved in completion")
    }
}