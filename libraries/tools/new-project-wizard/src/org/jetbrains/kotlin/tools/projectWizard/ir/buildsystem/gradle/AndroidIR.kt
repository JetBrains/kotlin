package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage

interface AndroidIR : GradleIR

//TODO parematrize
data class AndroidConfigIR(val javaPackage: JavaPackage) : AndroidIR, FreeIR {
    override fun GradlePrinter.renderGradle() {
        sectionCall("android", needIndent = true) {
            call("compileSdkVersion") { +"29" }; nlIndented() // TODO dehardcode
            sectionCall("defaultConfig", needIndent = true) {
                assignmentOrCall("applicationId") { +javaPackage.asCodePackage().quotified }; nlIndented()
                call("minSdkVersion") { +"24" }; nlIndented()  // TODO dehardcode
                call("targetSdkVersion") { +"29" }; nlIndented() // TODO dehardcode
                assignmentOrCall("versionCode") { +"1" }; nlIndented()
                assignmentOrCall("versionName") { +"1.0".quotified }
            }
            nlIndented()
            sectionCall("buildTypes", needIndent = true) {
                val sectionIdentifier = when (dsl) {
                    GradlePrinter.GradleDsl.KOTLIN -> """getByName("release")"""
                    GradlePrinter.GradleDsl.GROOVY -> "release".quotified
                }
                sectionCall(sectionIdentifier, needIndent = true) {
                    assignmentOrCall("isMinifyEnabled") { +"false" }
                }
            }
        }
    }
}
