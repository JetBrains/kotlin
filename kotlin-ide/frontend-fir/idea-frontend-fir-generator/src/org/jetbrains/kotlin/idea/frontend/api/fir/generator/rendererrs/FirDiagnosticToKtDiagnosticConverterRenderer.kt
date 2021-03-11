/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs

import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.*
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

object FirDiagnosticToKtDiagnosticConverterRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticConverter(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticConverter(diagnosticList: HLDiagnosticList) {
        inBracketsWithIndent("internal val KT_DIAGNOSTIC_CONVERTER = KtDiagnosticConverterBuilder.buildConverter") {
            for (diagnostic in diagnosticList.diagnostics) {
                printConverter(diagnostic)
            }
        }
    }

    private fun SmartPrinter.printConverter(diagnostic: HLDiagnostic) {
        println("add(FirErrors.${diagnostic.original.name}) { firDiagnostic ->")
        withIndent {
            println("${diagnostic.implClassName}(")
            withIndent {
                printDiagnosticParameters(diagnostic)
            }
            println(")")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic) {
        printCustomParameters(diagnostic)
        println("firDiagnostic as FirPsiDiagnostic<*>,")
        println("token,")
    }


    private fun SmartPrinter.printCustomParameters(diagnostic: HLDiagnostic) {
        diagnostic.parameters.forEach { parameter ->
            printParameter(parameter)
        }
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter) {
        val expression = parameter.conversion.convertExpression(
            "firDiagnostic.${parameter.originalParameterName}",
            ConversionContext(getCurrentIndentInUnits(), getIndentUnit())
        )
        println("$expression,")
    }

    override fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String> =
        diagnosticParameter.importsToAdd

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors",
    )
}
