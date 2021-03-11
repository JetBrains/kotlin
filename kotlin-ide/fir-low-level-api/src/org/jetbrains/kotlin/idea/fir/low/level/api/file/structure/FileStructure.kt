/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import java.util.concurrent.ConcurrentHashMap


internal class FileStructure(
    private val ktFile: KtFile,
    private val firFile: FirFile,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    private val firFileBuilder: FirFileBuilder,
    private val moduleFileCache: ModuleFileCache,
    private val collector: FirTowerDataContextCollector
) {
    private val firIdeProvider = firFile.session.firIdeProvider

    private val structureElements = ConcurrentHashMap<KtAnnotated, FileStructureElement>()

    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val container: KtAnnotated = element.getNonLocalContainingOrThisDeclaration() ?: element.containingKtFile
        return getStructureElementForDeclaration(container)
    }

    private fun getStructureElementForDeclaration(declaration: KtAnnotated): FileStructureElement {
        @Suppress("CANNOT_CHECK_FOR_ERASED")
        val structureElement = structureElements.compute(declaration) { _, structureElement ->
            when {
                structureElement == null -> createStructureElement(declaration)
                structureElement is ReanalyzableStructureElement<KtDeclaration> && !structureElement.isUpToDate() -> {
                    structureElement.reanalyze(declaration as KtDeclaration, moduleFileCache, firLazyDeclarationResolver, firIdeProvider)
                }
                else -> structureElement
            }
        }
        return structureElement
            ?: error("FileStructureElement for was not defined for \n${declaration.getElementTextInContext()}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getAllDiagnosticsForFile(diagnosticCheckerFilter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> {
        val structureElements = getAllStructureElements()
        return buildSet {
            collectDiagnosticsFromStructureElements(structureElements, diagnosticCheckerFilter)
        }
    }

    private fun MutableSet<FirPsiDiagnostic<*>>.collectDiagnosticsFromStructureElements(
        structureElements: Collection<FileStructureElement>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ) {
        structureElements.forEach { structureElement ->
            structureElement.diagnostics.forEach(diagnosticCheckerFilter) { diagnostics ->
                addAll(diagnostics)
            }
        }
    }

    private fun getAllStructureElements(): Collection<FileStructureElement> {
        val structureElements = mutableSetOf(getStructureElementFor(ktFile))
        ktFile.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                val structureElement = getStructureElementFor(dcl)
                structureElements += structureElement
                if (structureElement !is ReanalyzableStructureElement<*>) {
                    dcl.acceptChildren(this)
                }
            }
        })

        return structureElements
    }


    private fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(
            firFileBuilder,
            firIdeProvider.symbolProvider,
            moduleFileCache,
            firFile
        )
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclaration,
            moduleFileCache,
            FirResolvePhase.BODY_RESOLVE,
            checkPCE = true,
            towerDataContextCollector = collector
        )
        return moduleFileCache.firFileLockProvider.withReadLock(firFile) {
            FileElementFactory.createFileStructureElement(firDeclaration, declaration, firFile)
        }
    }

    private fun createStructureElement(container: KtAnnotated): FileStructureElement = when (container) {
        is KtFile -> {
            val firFile = firFileBuilder.getFirFileResolvedToPhaseWithCaching(
                container,
                moduleFileCache,
                //TODO: Make resolve whole file into TYPES only for top level declarations or annotations with `file` site
                FirResolvePhase.TYPES,
                checkPCE = true
            )
            RootStructureElement(
                firFile,
                container,
            )
        }
        is KtDeclaration -> createDeclarationStructure(container)
        else -> error("Invalid container $container")
    }
}
