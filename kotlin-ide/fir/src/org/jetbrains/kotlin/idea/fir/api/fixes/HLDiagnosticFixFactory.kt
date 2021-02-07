/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi

sealed class HLDiagnosticFixFactory<DIAGNOSTIC_PSI : PsiElement, in DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    val applicator: HLApplicator<TARGET_PSI, INPUT>
) {
    abstract fun KtAnalysisSession.createTargets(diagnostic: DIAGNOSTIC): List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
}

private class HLDiagnosticFixFactoryImpl<DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    applicator: HLApplicator<TARGET_PSI, INPUT>,
    private val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
) : HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT>(applicator) {
    override fun KtAnalysisSession.createTargets(diagnostic: DIAGNOSTIC): List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>> =
        createQuickFixes.invoke(this, diagnostic)
}

internal fun <DIAGNOSTIC : KtDiagnosticWithPsi<PsiElement>> KtAnalysisSession.createPlatformQuickFixes(
    diagnostic: DIAGNOSTIC,
    factory: HLDiagnosticFixFactory<PsiElement, DIAGNOSTIC, PsiElement, HLApplicatorInput>
): List<IntentionAction> = with(factory) {
    createTargets(diagnostic).map { (target, input) -> HLQuickFix(target, input, factory.applicator) }
}

fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> diagnosticFixFactory(
    applicator: HLApplicator<TARGET_PSI, INPUT>,
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
): HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT> =
    HLDiagnosticFixFactoryImpl(applicator, createQuickFixes)