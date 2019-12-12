package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter

interface MavenIR : BuildSystemIR {
    fun MavenPrinter.render()

    override fun BuildFilePrinter.render() {
        if (this is MavenPrinter) render()
    }
}

data class ModulesDependencyMavenIR(val dependencies: List<String>) : MavenIR {
    override fun MavenPrinter.render() {
        node("modules") {
            dependencies.forEach { dependency ->
                singleLineNode("module") { +dependency }
            }
        }
    }
}


