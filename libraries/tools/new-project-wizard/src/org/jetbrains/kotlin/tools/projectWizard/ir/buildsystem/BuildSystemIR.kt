package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.ir.IR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter

interface BuildSystemIR : IR {
    fun BuildFilePrinter.render()
}

// IR element, which is not hardcoded into the parent IR structure and can be printed
// In any place of the parent IR
interface FreeIR : BuildSystemIR

interface KotlinIR : BuildSystemIR

fun BuildSystemIR.render(printer: BuildFilePrinter) {
    with(printer) { render() }
}