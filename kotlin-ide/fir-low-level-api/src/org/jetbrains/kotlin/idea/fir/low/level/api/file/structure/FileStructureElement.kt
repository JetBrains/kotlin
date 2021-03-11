/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileStructureElementDiagnosticList
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileStructureElementDiagnosticRetriever
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileStructureElementDiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.hasFqName
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isGeneratedDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ktDeclaration
import org.jetbrains.kotlin.psi.*

internal sealed class FileStructureElement(val firFile: FirFile) {
    abstract val psi: KtAnnotated
    abstract val mappings: Map<KtElement, FirElement>
    abstract val diagnostics: FileStructureElementDiagnostics
}

internal sealed class ReanalyzableStructureElement<KT : KtDeclaration>(firFile: FirFile) : FileStructureElement(firFile) {
    abstract override val psi: KtDeclaration
    abstract val firSymbol: AbstractFirBasedSymbol<*>
    abstract val timestamp: Long

    /**
     * Creates new declaration by [newKtDeclaration] which will serve as replacement of [firSymbol]
     * Also, modify [firFile] & replace old version of declaration with a new one
     */
    abstract fun reanalyze(
        newKtDeclaration: KT,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzableStructureElement<KT>

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics = FileStructureElementDiagnostics(firFile, FileStructureElementSingleDeclarationDiagnosticRetriever())

    inner class FileStructureElementSingleDeclarationDiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
        override fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList {
            var inCurrentDeclaration = false
            val declaration = firSymbol.fir as FirDeclaration
            return collector.collectForStructureElement(
                firFile,
                onDeclarationEnter = { firDeclaration ->
                    when {
                        firDeclaration == declaration -> {
                            inCurrentDeclaration = true
                            DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        }
                        inCurrentDeclaration -> DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        else -> DiagnosticCollectorDeclarationAction.DO_NOT_CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                    }
                },
                onDeclarationExit = { firDeclaration ->
                    if (declaration == firDeclaration) {
                        inCurrentDeclaration = false
                    }
                }
            )
        }
    }

    companion object {
        val recorder = FirElementsRecorder()
    }
}

internal class ReanalyzableFunctionStructureElement(
    firFile: FirFile,
    override val psi: KtNamedFunction,
    override val firSymbol: FirFunctionSymbol<*>,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtNamedFunction>(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtNamedFunction,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzableFunctionStructureElement {
        val originalFunction = firSymbol.fir as FirSimpleFunction
        val newFunction = firIdeProvider.buildFunctionWithBody(newKtDeclaration, originalFunction) as FirSimpleFunction

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalFunction, newFunction) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newFunction,
                cache,
                FirResolvePhase.BODY_RESOLVE,
                checkPCE = true,
                reresolveFile = true,
            )
            cache.firFileLockProvider.withReadLock(firFile) {
                ReanalyzableFunctionStructureElement(
                    firFile,
                    newKtDeclaration,
                    newFunction.symbol,
                    newKtDeclaration.modificationStamp,
                )
            }
        }
    }
}

internal class ReanalyzablePropertyStructureElement(
    firFile: FirFile,
    override val psi: KtProperty,
    override val firSymbol: FirPropertySymbol,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtProperty>(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtProperty,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzablePropertyStructureElement {
        val originalProperty = firSymbol.fir
        val newProperty = firIdeProvider.buildPropertyWithBody(newKtDeclaration, originalProperty)

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalProperty, newProperty) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newProperty,
                cache,
                FirResolvePhase.BODY_RESOLVE,
                checkPCE = true,
                reresolveFile = true,
            )
            cache.firFileLockProvider.withReadLock(firFile) {
                ReanalyzablePropertyStructureElement(
                    firFile,
                    newKtDeclaration,
                    newProperty.symbol,
                    newKtDeclaration.modificationStamp,
                )
            }
        }
    }
}

internal class NonReanalyzableDeclarationStructureElement(
    firFile: FirFile,
    private val fir: FirDeclaration,
    override val psi: KtDeclaration,
) : FileStructureElement(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(fir, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, DiagnosticRetriever())

    private inner class DiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
        override fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList {
            var inCurrentDeclaration = false
            return collector.collectForStructureElement(
                firFile,
                onDeclarationEnter = { firDeclaration ->
                    when {
                        // Some generated declaration contains structures that we need to check. For example the FIR representation of an
                        // enum entry initializer, when present, is a generated anonymous object of kind `ENUM_ENTRY`.
                        firDeclaration.isGeneratedDeclaration ->
                            DiagnosticCollectorDeclarationAction.DO_NOT_CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        firDeclaration is FirFile -> DiagnosticCollectorDeclarationAction.DO_NOT_CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        firDeclaration == fir -> {
                            inCurrentDeclaration = true
                            DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        }
                        FileElementFactory.isReanalyzableContainer(firDeclaration.ktDeclaration) -> {
                            DiagnosticCollectorDeclarationAction.SKIP
                        }
                        inCurrentDeclaration -> {
                            DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                        }
                        else -> DiagnosticCollectorDeclarationAction.DO_NOT_CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED
                    }
                },
                onDeclarationExit = { firDeclaration ->
                    if (firDeclaration == fir) {
                        inCurrentDeclaration = false
                    }
                },
            )
        }
    }


    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
                val psi = property.psi as? KtProperty ?: return super.visitProperty(property, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || !psi.hasFqName()) {
                    super.visitProperty(property, data)
                }
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
                val psi = simpleFunction.psi as? KtNamedFunction ?: return super.visitSimpleFunction(simpleFunction, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || !psi.hasFqName()) {
                    super.visitSimpleFunction(simpleFunction, data)
                }
            }
        }
    }
}


internal class RootStructureElement(
    firFile: FirFile,
    override val psi: KtFile,
) : FileStructureElement(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firFile, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, DiagnosticRetriever)

    private object DiagnosticRetriever : FileStructureElementDiagnosticRetriever() {
        override fun retrieve(firFile: FirFile, collector: FileStructureElementDiagnosticsCollector): FileStructureElementDiagnosticList {
            return collector.collectForStructureElement(firFile) { firDeclaration ->
                if (firDeclaration is FirFile) DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_DO_NOT_LOOKUP_FOR_NESTED
                else DiagnosticCollectorDeclarationAction.SKIP
            }
        }
    }

    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
                if (element !is FirDeclaration || element is FirFile) {
                    super.visitElement(element, data)
                }
            }
        }
    }
}
