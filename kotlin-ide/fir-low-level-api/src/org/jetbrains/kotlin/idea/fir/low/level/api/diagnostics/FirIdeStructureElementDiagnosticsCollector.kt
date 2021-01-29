/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureElementDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement

internal class FirIdeStructureElementDiagnosticsCollector private constructor(
    session: FirSession,
    private val onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction,
    private val onDeclarationExit: (FirDeclaration) -> Unit
) : AbstractFirIdeDiagnosticsCollector(
    session,
) {
    private val result = mutableMapOf<PsiElement, MutableList<FirPsiDiagnostic<*>>>()

    override fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>) {
        result.addValueFor(diagnostic.psiElement, diagnostic)
    }

    override fun onDeclarationEnter(
        declaration: FirDeclaration,
    ): DiagnosticCollectorDeclarationAction =
        onDeclarationEnter.invoke(declaration)

    override fun onDeclarationExit(declaration: FirDeclaration) {
        onDeclarationExit.invoke(declaration)
    }


    companion object {
        fun collectForStructureElement(
            firFile: FirFile,
            onDeclarationExit: (FirDeclaration) -> Unit = {},
            onDeclarationEnter: (FirDeclaration) -> DiagnosticCollectorDeclarationAction,
        ): FileStructureElementDiagnostics =
            FirIdeStructureElementDiagnosticsCollector(firFile.session, onDeclarationEnter, onDeclarationExit).let { collector ->
                collector.collectDiagnostics(firFile)
                FileStructureElementDiagnostics(collector.result)
            }

        fun collectForSingleDeclaration(firFile: FirFile, declaration: FirDeclaration): FileStructureElementDiagnostics {
            var inCurrentDeclaration = false

            return collectForStructureElement(
                firFile,
                onDeclarationEnter = { firDeclaration ->
                    when {
                        firDeclaration == declaration -> {
                            inCurrentDeclaration = true
                            DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                        }
                        inCurrentDeclaration -> DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                        else -> DiagnosticCollectorDeclarationAction.SKIP_CURRENT_DECLARATION_AND_CHECK_NESTED
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
}
