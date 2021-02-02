/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs

import org.jetbrains.kotlin.fir.checkers.generator.collectClassNamesTo
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticListRenderer
import org.jetbrains.kotlin.fir.checkers.generator.printImports
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.fir.tree.generator.printer.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.printer.printGeneratedMessage
import org.jetbrains.kotlin.fir.tree.generator.printer.useSmartPrinter
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.HLDiagnosticConverter
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.HLDiagnosticList
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.HLDiagnosticParameter
import java.io.File

abstract class AbstractDiagnosticsDataClassRenderer : DiagnosticListRenderer() {
    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String) {
        val hlDiagnosticsList = HLDiagnosticConverter.convert(diagnosticList)
        file.useSmartPrinter { render(hlDiagnosticsList, packageName) }
    }

    private fun SmartPrinter.collectAndPrintImports(diagnosticList: HLDiagnosticList) {
        val imports = collectImports(diagnosticList)
        printImports(imports)
    }

    protected fun SmartPrinter.printHeader(packageName: String, diagnosticList: HLDiagnosticList) {
        printCopyright()
        println("package $packageName")
        println()
        collectAndPrintImports(diagnosticList)
        println()
        printGeneratedMessage()
    }

    @OptIn(ExperimentalStdlibApi::class)
    protected fun collectImports(diagnosticList: HLDiagnosticList): Collection<String> = buildSet {
        addAll(defaultImports)
        for (diagnostic in diagnosticList.diagnostics) {
            diagnostic.original.psiType.collectClassNamesTo(this)
            diagnostic.parameters.forEach { diagnosticParameter ->
                addAll(collectImportsForDiagnosticParameter(diagnosticParameter))
            }
        }
    }

    protected abstract fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String>

    protected abstract fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String)

    protected abstract val defaultImports: Collection<String>
}