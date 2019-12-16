package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

interface GradleTaskAccessIR : GradleIR {
    val name: String
}

data class GradleByNameTaskAccessIR(
    override val name: String,
    val taskClass: String? = null
) : GradleTaskAccessIR {
    override fun GradlePrinter.renderGradle() {
        when (dsl) {
            GradlePrinter.GradleDsl.GROOVY -> {
                +"tasks.getByName('$name')"
            }
            GradlePrinter.GradleDsl.KOTLIN -> {
                +"tasks.getByName"
                taskClass?.let { +"<$it>" }
                +"(${name.quotified})"
            }
        }
    }
}

data class GradleConfigureTaskIR(
    val taskAccess: GradleTaskAccessIR,
    val dependsOn: List<BuildSystemIR> = emptyList(),
    val irs: List<BuildSystemIR> = emptyList()
) : GradleIR, FreeIR {
    override fun GradlePrinter.renderGradle() {
        taskAccess.render(this)
        +" "
        inBrackets {
            if (dependsOn.isNotEmpty()) {
                indent()
                call("dependsOn", forceBrackets = true) {
                    dependsOn.list { it.render(this) }
                }
                nl()
            }
            irs.listNl()
        }
    }
}
