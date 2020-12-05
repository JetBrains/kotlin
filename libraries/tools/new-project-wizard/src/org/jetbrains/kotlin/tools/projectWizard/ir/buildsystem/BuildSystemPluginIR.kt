package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.ignore
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

interface BuildSystemPluginIR : BuildSystemIR, BuildSystemIRWithPriority

interface DefaultBuildSystemPluginIR : BuildSystemPluginIR

data class ApplicationPluginIR(val mainClass: String) : DefaultBuildSystemPluginIR {
    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> when (dsl) {
            GradlePrinter.GradleDsl.KOTLIN -> +"application"
            GradlePrinter.GradleDsl.GROOVY -> +"id 'application'"
        }
        is MavenPrinter -> node("plugin") {
            singleLineNode("groupId") { +"org.codehaus.mojo" }
            singleLineNode("artifactId") { +"exec-maven-plugin" }
            singleLineNode("version") { +"1.6.0" }

            node("configuration") {
                singleLineNode("mainClass") { +mainClass }
            }
        }
        else -> Unit
    }
}

data class GradleOnlyPluginByNameIR(
    @NonNls val pluginId: String,
    val version: Version? = null,
    override val priority: Int? = null,
) : BuildSystemPluginIR, GradleIR {
    override fun GradlePrinter.renderGradle() {
        call("id") { +pluginId.quotified }
        version?.let { version ->
            +" version "
            +version.text.quotified
        }
    }
}

data class KotlinBuildSystemPluginIR(
    val type: Type,
    val version: WizardKotlinVersion?,
    override val priority: Int? = null
) : BuildSystemPluginIR {

    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> {
            when (dsl) {
                GradlePrinter.GradleDsl.KOTLIN -> call("kotlin") { +type.toString().quotified }
                GradlePrinter.GradleDsl.GROOVY -> call("id") { +"org.jetbrains.kotlin.$type".quotified }
            }
            version?.version?.let {
                +" version "
                +it.toString().quotified
            }.ignore()
        }
        is MavenPrinter -> node("plugin") {
            singleLineNode("groupId") { +"org.jetbrains.kotlin" }
            singleLineNode("artifactId") { +"kotlin-maven-plugin" }
            singleLineNode("version") { +version?.version.toString() }

            node("executions") {
                node("execution") {
                    singleLineNode("id") { +"compile" }
                    singleLineNode("phase") { +"compile" }
                    node("goals") {
                        singleLineNode("goal") { +"compile" }
                    }
                }

                node("execution") {
                    singleLineNode("id") { +"test-compile" }
                    singleLineNode("phase") { +"test-compile" }
                    node("goals") {
                        singleLineNode("goal") { +"test-compile" }
                    }
                }
            }
        }
        else -> Unit
    }

    @Suppress("EnumEntryName", "unused", "SpellCheckingInspection")
    enum class Type {
        jvm, multiplatform, android, js
    }
}