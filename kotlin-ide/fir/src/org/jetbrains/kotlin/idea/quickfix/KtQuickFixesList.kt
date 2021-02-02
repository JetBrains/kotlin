/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import kotlin.reflect.KClass

@RequiresOptIn
annotation class ForKtQuickFixesListBuilder()


class KtQuickFixesListBuilder private constructor() {
    val quickFixes = mutableMapOf<KClass<out KtDiagnosticWithPsi>, MutableList<QuickFixFactory>>()

    inline fun <reified D : KtDiagnosticWithPsi> register(quickFixFactory: QuickFixFactory) {
        quickFixes.getOrPut(D::class) { mutableListOf() }.add(quickFixFactory)
    }

    @OptIn(ForKtQuickFixesListBuilder::class)
    private fun build() = KtQuickFixesList(quickFixes)

    companion object {
        fun register(init: KtQuickFixesListBuilder.() -> Unit) = KtQuickFixesListBuilder().apply(init).build()
    }
}

class KtQuickFixesList @ForKtQuickFixesListBuilder constructor(private val quickFixes: Map<KClass<out KtDiagnosticWithPsi>, List<QuickFixFactory>>) {
    fun getQuickFixesFor(diagnostic: KtDiagnosticWithPsi): List<IntentionAction> {
        val factories = quickFixes[diagnostic.diagnosticClass] ?: return emptyList()
        return factories.mapNotNull { it.createQuickFix(diagnostic) }
    }

    private fun QuickFixFactory.createQuickFix(
        diagnostic: KtDiagnosticWithPsi
    ) = when (this) {
        is QuickFixesPsiBasedFactory -> createQuickFix(diagnostic.psi)
        else -> error("Unsupported QuickFixFactory $this")
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

private fun <K, V> List<Map<K, List<V>>>.merge(): Map<K, List<V>> {
    return flatMap { it.entries }
        .groupingBy { it.key }
        .aggregate<Map.Entry<K, List<V>>, K, MutableList<V>> { _, accumulator, element, _ ->
            val list = accumulator ?: mutableListOf()
            list.addAll(element.value)
            list
        }
}