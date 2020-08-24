/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getContainingFile
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal

/**
 * Maps [KtElement] to [FirElement]
 * Stateless, caches everything into [ModuleFileCache] & [PsiToFirCache] passed into the function
 */
@ThreadSafe
internal class FirElementBuilder(
    private val firFileBuilder: FirFileBuilder,
) {
    fun getOrBuildFirFor(
        element: KtElement,
        moduleFileCache: ModuleFileCache,
        psiToFirCache: PsiToFirCache,
        toPhase: FirResolvePhase,
    ): FirElement {
        val ktFile = element.containingKtFile
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)

        val containerFir = when (val container = element.getNonLocalContainingDeclarationWithFqName()) {
            is KtDeclaration -> {
                FirElementFinder.findElementByPsiIn<FirDeclaration>(firFile, container)
                    ?: error("Declaration was not found in FIR file which was build by declaration KtFile, declaration is\n${container.getElementTextInContext()}")
            }
            null -> firFile
            else -> error("Unsupported: ${container.text}")
        }

        runLazyResolveWithPCECheck(containerFir, moduleFileCache, firFile, firFile.session.firIdeProvider, toPhase)

        return psiToFirCache.getFir(element, containerFir, firFile)
    }

    fun lazyResolveDeclarationWithPCECheck(
        declaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase
    ) {
        if (declaration.resolvePhase < toPhase) {
            val firFile = declaration.getContainingFile() ?: error("FirFile was not found for\n${declaration.render()}")
            runLazyResolveWithPCECheck(declaration, moduleFileCache, firFile, firFile.session.firIdeProvider, toPhase)
        }
    }

    private fun runLazyResolvePhase(
        firDeclarationToResolve: FirDeclaration,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>?,
    ) {
        val nonLocalDeclarationToResolve = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider)

        val designation = mutableListOf<FirDeclaration>(containerFirFile)
        if (nonLocalDeclarationToResolve !is FirFile) {
            val id = when (nonLocalDeclarationToResolve) {
                is FirCallableDeclaration<*> -> {
                    nonLocalDeclarationToResolve.symbol.callableId.classId
                }
                is FirRegularClass -> {
                    nonLocalDeclarationToResolve.symbol.classId
                }
                else -> error("Unsupported: ${nonLocalDeclarationToResolve.render()}")
            }
            val outerClasses = generateSequence(id) { classId ->
                classId.outerClassId
            }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it)!! }
            designation += outerClasses.asReversed()
            if (nonLocalDeclarationToResolve is FirCallableDeclaration<*>) {
                designation += nonLocalDeclarationToResolve
            }
        }
        if (designation.all { it.resolvePhase >= toPhase }) {
            return
        }
        val scopeSession = ScopeSession()
        val transformer = FirDesignatedBodyResolveTransformerForIDE(
            designation.iterator(), containerFirFile.session,
            scopeSession,
            implicitTypeOnly = toPhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            towerDataContextForStatement
        )
        containerFirFile.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
    }

    /**
     * Should be invoked under lock
     */
    private fun runLazyResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>? = null,
    ) {
        val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
        if (firDeclarationToResolve.resolvePhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLockNoPCECheck(
                containerFirFile,
                fromPhase = firDeclarationToResolve.resolvePhase,
                toPhase = nonLazyPhase
            )
        }
        if (toPhase <= nonLazyPhase) return
        runLazyResolvePhase(firDeclarationToResolve, containerFirFile, provider, toPhase, towerDataContextForStatement)
    }

    fun runLazyResolveWithPCECheck(
        firDeclarationToResolve: FirDeclaration,
        cache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>? = null,
    ) {
        val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
        if (firDeclarationToResolve.resolvePhase < nonLazyPhase) {
            firFileBuilder.runResolveWithPCECheck(
                containerFirFile,
                fromPhase = firDeclarationToResolve.resolvePhase,
                toPhase = nonLazyPhase,
                cache = cache,
            )
        }
        if (toPhase <= nonLazyPhase) return
        firFileBuilder.runCustomResolveWithPCECheck(containerFirFile, cache) {
            runLazyResolvePhase(firDeclarationToResolve, containerFirFile, provider, toPhase, towerDataContextForStatement)
        }
    }

    //TODO for completion only
    fun runLazyResolveForCompletion(
        firFunction: FirFunction<*>,
        containerFirFile: FirFile,
        firIdeProvider: FirIdeProvider,
        toPhase: FirResolvePhase,
        towerDataContextForStatement: MutableMap<FirStatement, FirTowerDataContext>
    ) {
        runLazyResolveWithoutLock(firFunction, containerFirFile, firIdeProvider, toPhase, towerDataContextForStatement)
    }
}

private fun FirDeclaration.getNonLocalDeclarationToResolve(provider: FirProvider): FirDeclaration {
    if (this is FirFile) return this
    val ktDeclaration = psi as? KtDeclaration ?: error("FirDeclaration should have a PSI of type KtDeclaration")
    if (!KtPsiUtil.isLocal(ktDeclaration)) return this
    return when (val nonLocalPsi = ktDeclaration.getNonLocalContainingDeclarationWithFqName()) {
        is KtClassOrObject -> provider.getFirClassifierByFqName(
            nonLocalPsi.classIdIfNonLocal()
                ?: error("Container classId should not be null for non-local declaration")
        ) ?: error("Could not find class ${nonLocalPsi.classIdIfNonLocal()}")
        is KtProperty, is KtNamedFunction -> {
            val containerClass = nonLocalPsi.containingClassOrObject
                ?: error("Container class should not be null for non-local declaration")
            val containerClassId = containerClass.classIdIfNonLocal()
                ?: error("Container classId should not be null for non-local declaration")
            val containingFir = provider.getFirClassifierByFqName(containerClassId) as? FirRegularClass
                ?: error("Could not find class $containerClassId")
            containingFir.declarations.first { it.psi === nonLocalPsi }
        }
        else -> error("Invalid container ${nonLocalPsi?.let { it::class } ?: "null"}")
    }
}


private fun KtElement.getNonLocalContainingDeclarationWithFqName(): KtNamedDeclaration? {
    var container = parent
    while (container != null && container !is KtFile) {
        if (container is KtNamedDeclaration
            && (container is KtClassOrObject || container is KtDeclarationWithBody)
            && !KtPsiUtil.isLocal(container)
            && container.name != null
            && container !is KtEnumEntry
            && container.containingClassOrObject !is KtEnumEntry
        ) {
            return container
        }
        container = container.parent
    }
    return null
}