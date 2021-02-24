package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

interface GradleTaskAccessIR : GradleIR

data class GradleByNameTaskAccessIR(
    val name: String,
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

data class GradleByClassTasksAccessIR(
    @NonNls val taskClass: String
) : GradleTaskAccessIR {
    override fun GradlePrinter.renderGradle() {
        when (dsl) {
            GradlePrinter.GradleDsl.GROOVY -> {
                +"tasks.withType($taskClass)"
            }
            GradlePrinter.GradleDsl.KOTLIN -> {
                +"tasks.withType<$taskClass>()"
            }
        }
    }
}

data class GradleByClassTasksCreateIR(
    val taskName: String,
    val taskClass: String
) : GradleTaskAccessIR {
    override fun GradlePrinter.renderGradle() {
        when (dsl) {
            GradlePrinter.GradleDsl.KOTLIN -> {
                +"val $taskName by tasks.creating($taskClass::class)"
            }
            GradlePrinter.GradleDsl.GROOVY -> {
                +"task($taskName, type: $taskClass)"
            }
        }
    }
}


data class GradleConfigureTaskIR(
    val taskAccess: GradleTaskAccessIR,
    val dependsOn: List<BuildSystemIR> = emptyList(),
    val irs: List<BuildSystemIR> = emptyList()
) : GradleIR, FreeIR {
    constructor(
        taskAccess: GradleTaskAccessIR,
        dependsOn: List<BuildSystemIR> = emptyList(),
        createIrs: IRsListBuilderFunction
    ) : this(taskAccess, dependsOn, createIrs.build())

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
