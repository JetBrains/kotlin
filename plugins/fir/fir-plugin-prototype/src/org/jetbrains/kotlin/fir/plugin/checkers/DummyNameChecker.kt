/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction

object DummyNameChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirSimpleFunction) return
        if (declaration.name.asString() == "dummy") {
            reporter.reportOn(declaration.source, FirErrors.SYNTAX, context)
        }
    }
}
