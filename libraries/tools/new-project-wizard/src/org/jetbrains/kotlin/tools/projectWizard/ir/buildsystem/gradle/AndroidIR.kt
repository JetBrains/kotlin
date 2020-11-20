package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.asStringWithUnixSlashes
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.FreeIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import java.nio.file.Path

interface AndroidIR : GradleIR

//TODO parematrize
data class AndroidConfigIR(
    val javaPackage: JavaPackage?,
    val newManifestPath: Path?,
    val printVersionCode: Boolean,
    val printBuildTypes: Boolean,
) : AndroidIR, FreeIR {
    override fun GradlePrinter.renderGradle() {
        sectionCall("android", needIndent = true) {
            call("compileSdkVersion") { +"29" }; nlIndented() // TODO dehardcode
            if (newManifestPath != null) {
                when (dsl) {
                    GradlePrinter.GradleDsl.KOTLIN -> {
                        +"""sourceSets["main"].manifest.srcFile("${newManifestPath.asStringWithUnixSlashes()}")"""
                    }
                    GradlePrinter.GradleDsl.GROOVY -> {
                        +"""sourceSets.main.manifest.srcFile('${newManifestPath.asStringWithUnixSlashes()}')"""
                    }
                }
                nlIndented()
            }
            sectionCall("defaultConfig", needIndent = true) {
                if (javaPackage != null) {
                    assignmentOrCall("applicationId") { +javaPackage.asCodePackage().quotified }; nlIndented()
                }
                call("minSdkVersion") { +"24" }; nlIndented()  // TODO dehardcode
                call("targetSdkVersion") { +"29" };// TODO dehardcode
                if (printVersionCode) {
                    nlIndented()
                    assignmentOrCall("versionCode") { +"1" }; nlIndented()
                    assignmentOrCall("versionName") { +"1.0".quotified }
                }
            }
            if (printBuildTypes) {
                nlIndented()
                sectionCall("buildTypes", needIndent = true) {
                    val sectionIdentifier = when (dsl) {
                        GradlePrinter.GradleDsl.KOTLIN -> """getByName("release")"""
                        GradlePrinter.GradleDsl.GROOVY -> "release".quotified
                    }
                    sectionCall(sectionIdentifier, needIndent = true) {
                        val minifyCallName = when (dsl) {
                            GradlePrinter.GradleDsl.KOTLIN -> "isMinifyEnabled"
                            GradlePrinter.GradleDsl.GROOVY -> "minifyEnabled"
                        }

                        assignmentOrCall(minifyCallName) { +"false" }
                    }
                }
            }
        }
    }
}
