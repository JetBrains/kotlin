package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.ListBuilder
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

data class BodyIR(
    val body: List<BuildSystemIR>
) : GradleIR {
    constructor(vararg body: BuildSystemIR) : this(body.toList())

    override fun GradlePrinter.renderGradle() {
        inBrackets {
            body.listNl()
        }
    }
}


fun buildBody(builder: ListBuilder<BuildSystemIR>.() -> Unit) =
    BodyIR(buildList(builder))

fun BodyIR.renderIfNotEmpty(printer: GradlePrinter) =
    takeIf { it.isNotEmpty() }?.render(printer)

fun BodyIR.isEmpty() = body.isEmpty()
fun BodyIR.isNotEmpty() = body.isNotEmpty()
