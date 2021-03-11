/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.quickfix.QuickFixesPsiBasedFactory
import kotlin.reflect.KClass

class KtQuickFixesList @ForKtQuickFixesListBuilder @OptIn(PrivateForInline::class) constructor(
    private val quickFixes: Map<KClass<out KtDiagnosticWithPsi<*>>, List<HLQuickFixFactory>>
) {
    fun KtAnalysisSession.getQuickFixesFor(diagnostic: KtDiagnosticWithPsi<*>): List<IntentionAction> {
        val factories = quickFixes[diagnostic.diagnosticClass] ?: return emptyList()
        return factories.flatMap { createQuickFixes(it, diagnostic) }
    }

    @OptIn(PrivateForInline::class)
    private fun KtAnalysisSession.createQuickFixes(
        quickFixFactory: HLQuickFixFactory,
        diagnostic: KtDiagnosticWithPsi<*>
    ): List<IntentionAction> = when (quickFixFactory) {
        is HLQuickFixFactory.HLApplicatorBasedFactory -> {
            @Suppress("UNCHECKED_CAST")
            val factory = quickFixFactory.applicatorFactory
                    as HLDiagnosticFixFactory<PsiElement, KtDiagnosticWithPsi<PsiElement>, PsiElement, HLApplicatorInput>
            createPlatformQuickFixes(diagnostic, factory)
        }
        is HLQuickFixFactory.HLQuickFixesPsiBasedFactory -> quickFixFactory.psiFactory.createQuickFix(diagnostic.psi)
    }


    companion object {
        @OptIn(ForKtQuickFixesListBuilder::class)
        fun createCombined(registrars: List<KtQuickFixesList>): KtQuickFixesList {
            val allQuickFixes = registrars.map { it.quickFixes }.merge()
            return KtQuickFixesList(allQuickFixes)
        }

        fun createCombined(vararg registrars: KtQuickFixesList): KtQuickFixesList {
            return createCombined(registrars.toList())
        }
    }
}


class KtQuickFixesListBuilder private constructor() {
    @OptIn(PrivateForInline::class)
    private val quickFixes = mutableMapOf<KClass<out KtDiagnosticWithPsi<*>>, MutableList<HLQuickFixFactory>>()

    @OptIn(PrivateForInline::class)
    fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> registerPsiQuickFixes(
        diagnosticClass: KClass<DIAGNOSTIC>,
        vararg quickFixFactories: QuickFixesPsiBasedFactory<in DIAGNOSTIC_PSI>
    ) {
        for (quickFixFactory in quickFixFactories) {
            registerPsiQuickFix(diagnosticClass, quickFixFactory)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <DIAGNOSTIC_PSI : PsiElement, reified DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> registerApplicator(
        quickFixFactory: HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT>
    ) {
        registerApplicator(DIAGNOSTIC::class, quickFixFactory)
    }

    @PrivateForInline
    fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> registerPsiQuickFix(
        diagnosticClass: KClass<DIAGNOSTIC>,
        quickFixFactory: QuickFixesPsiBasedFactory<in DIAGNOSTIC_PSI>
    ) {
        quickFixes.getOrPut(diagnosticClass) { mutableListOf() }.add(HLQuickFixFactory.HLQuickFixesPsiBasedFactory(quickFixFactory))
    }


    @PrivateForInline
    fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> registerApplicator(
        diagnosticClass: KClass<DIAGNOSTIC>,
        quickFixFactory: HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT>
    ) {
        quickFixes.getOrPut(diagnosticClass) { mutableListOf() }
            .add(HLQuickFixFactory.HLApplicatorBasedFactory(quickFixFactory))
    }

    @OptIn(ForKtQuickFixesListBuilder::class)
    private fun build() = KtQuickFixesList(quickFixes)

    companion object {
        fun registerPsiQuickFix(init: KtQuickFixesListBuilder.() -> Unit) = KtQuickFixesListBuilder().apply(init).build()
    }
}

@PrivateForInline
sealed class HLQuickFixFactory {
    class HLQuickFixesPsiBasedFactory(
        val psiFactory: QuickFixesPsiBasedFactory<*>
    ) : HLQuickFixFactory()

    class HLApplicatorBasedFactory(
        val applicatorFactory: HLDiagnosticFixFactory<*, *, *, *>
    ) : HLQuickFixFactory()
}


private fun <K, V> List<Map<K, List<V>>>.merge(): Map<K, List<V>> {
    return flatMap { it.entries }
        .groupingBy { it.key }
        .aggregate<Map.Entry<K, List<V>>, K, MutableList<V>> { _, accumulator, element, _ ->
            val list = accumulator ?: mutableListOf()
            list.addAll(element.value)
            list
        }
}

@RequiresOptIn
annotation class ForKtQuickFixesListBuilder()
