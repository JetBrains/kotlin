/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirDesignatedBodyResolveTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingDeclarationWithFqName
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getContainingFile
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
    fun lazyResolveDeclaration(
        declaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean = false
    ) {
        if (declaration.resolvePhase >= toPhase) return
        val firFile = declaration.getContainingFile()
            ?: error("FirFile was not found for\n${declaration.render()}")
        val provider = firFile.session.firIdeProvider
        if (checkPCE) {
            firFileBuilder.runCustomResolveWithPCECheck(firFile, moduleFileCache) {
                runLazyResolveWithoutLock(
                    declaration,
                    moduleFileCache,
                    firFile,
                    provider,
                    toPhase,
                    towerDataContextCollector,
                    checkPCE = true
                )
            }
        } else {
            firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
                executeWithoutPCE {
                    runLazyResolveWithoutLock(
                        declaration,
                        moduleFileCache,
                        firFile,
                        provider,
                        toPhase,
                        towerDataContextCollector,
                        checkPCE = false
                    )
                }
            }
        }
    }

    fun runLazyResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean
    ) {
        val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
        if (firDeclarationToResolve.resolvePhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLock(
                containerFirFile,
                fromPhase = firDeclarationToResolve.resolvePhase,
                toPhase = nonLazyPhase,
                checkPCE = checkPCE
            )
        }
        if (toPhase <= nonLazyPhase) return
        if (checkPCE) checkCanceled()
        runLazyResolvePhase(firDeclarationToResolve, containerFirFile, moduleFileCache, provider, toPhase, towerDataContextCollector)
    }

    private fun runLazyResolvePhase(
        firDeclarationToResolve: FirDeclaration,
        containerFirFile: FirFile,
        moduleFileCache: ModuleFileCache,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val nonLocalDeclarationToResolve = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider, moduleFileCache)

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
            towerDataContextCollector
        )
        containerFirFile.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
    }

    private fun FirDeclaration.getNonLocalDeclarationToResolve(provider: FirProvider, moduleFileCache: ModuleFileCache): FirDeclaration {
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
                if (containerClass != null) {
                    val containerClassId = containerClass.classIdIfNonLocal()
                        ?: error("Container classId should not be null for non-local declaration")
                    val containingFir = provider.getFirClassifierByFqName(containerClassId) as? FirRegularClass
                        ?: error("Could not find class $containerClassId")
                    containingFir.declarations.first { it.psi === nonLocalPsi }
                } else {
                    val packageFqName = ktDeclaration.containingKtFile.packageFqName
                    provider.symbolProvider.getTopLevelCallableSymbols(packageFqName, nonLocalPsi.nameAsSafeName)
                        .firstOrNull { it.fir.psi === nonLocalPsi }
                    val ktFile = ktDeclaration.containingKtFile
                    val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)
                    firFile.declarations.firstOrNull { it.psi === nonLocalPsi }
                        ?: error("Cannot find FIR for\n${nonLocalPsi.getElementTextInContext()}")
                }
            }
            else -> error("Invalid container ${nonLocalPsi?.let { it::class } ?: "null"}")
        }
    }
}


