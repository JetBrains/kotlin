/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.text

class UniqueDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :
    FirSimpleFunctionChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!config.checkUniqueness) return
        val errorCollector = ErrorCollector()
        try {
            val cfaChecker = UniqueCFA(UniqueCheckerData(session, config, errorCollector))
            declaration.controlFlowGraphReference?.controlFlowGraph?.let { cfaChecker.analyze(it, reporter, context) }
        } catch (e: Exception) {
            errorCollector.addErrorInfo("... while checking uniqueness level for ${declaration.source.text}")
            val error = errorCollector.formatErrorWithInfos(e.message ?: "No message provided")
            reporter.reportOn(declaration.source, PluginErrors.UNIQUENESS_VIOLATION, error, context)
        }
    }
}