package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

data class PomIR(
    val artifactId: String,
    val groupId: String,
    val version: Version
) : BuildSystemIR {
    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> {
            +"group = "; +groupId.quotified; nl()
            +"version = "; +version.toString().quotified; nl()
        }
        is MavenPrinter -> {
            singleLineNode("artifactId") { +artifactId }
            singleLineNode("groupId") { +groupId }
            singleLineNode("version") { +version.toString() }
        }
        else -> Unit
    }
}
