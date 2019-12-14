package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.ir.TaskIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

interface GradleTaskIR : GradleIR, TaskIR {
    val taskClass: String
    val body: BodyIR
}

data class CreateGradleTaskIR(
    override val name: String,
    override val taskClass: String,
    override val body: BodyIR
) : GradleTaskIR {
    override fun GradlePrinter.renderGradle() {
        when (dsl) {
            GradlePrinter.GradleDsl.GROOVY -> {
                +"tasks.create('$name', $taskClass)"
            }
            GradlePrinter.GradleDsl.KOTLIN -> {
                +"val $name by tasks.registering($taskClass::class)"
            }
        }
        +" "
        body.renderIfNotEmpty(this)
    }
}

data class GetGradleTaskIR(
    override val name: String,
    override val taskClass: String,
    override val body: BodyIR
) : GradleTaskIR {
    override fun GradlePrinter.renderGradle() {
        when (dsl) {
            GradlePrinter.GradleDsl.GROOVY -> {
                +"tasks.getByName('$name')"
            }
            GradlePrinter.GradleDsl.KOTLIN -> {
                +"tasks.getByName<$taskClass>(${name.quotified})"
            }
        }
        +" "
        body.renderIfNotEmpty(this)
    }
}