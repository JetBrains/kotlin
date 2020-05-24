/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.registerAllComponents
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement

class FirIdeDiagnosticsCollector(session: FirSession, private val resolveState: FirModuleResolveState) : AbstractDiagnosticCollector(session) {

    init {
        registerAllComponents()
    }

    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?) {
            if (diagnostic !is FirPsiDiagnostic<*>) return
            val psi = diagnostic.element.psi as? KtElement ?: return
            resolveState.record(psi, diagnostic.asPsiBasedDiagnostic())
        }
    }

    private lateinit var reporter: Reporter

    override fun initializeCollector() {
        reporter = Reporter()
    }

    override fun getCollectedDiagnostics(): Iterable<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }

    override fun runCheck(block: (DiagnosticReporter) -> Unit) {
        block(reporter)
    }
}