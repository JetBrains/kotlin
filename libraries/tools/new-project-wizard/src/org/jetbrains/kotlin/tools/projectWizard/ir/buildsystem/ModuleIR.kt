package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Path

sealed class ModuleIR : IrsOwner, BuildSystemIR {
    abstract val name: String
    abstract val path: Path
    abstract val template: Template?
    abstract val type: ModuleType
    abstract val originalModule: Module
}


interface SourcesetIR : BuildSystemIR {
    val sourcesetType: SourcesetType
    val path: Path
    val original: Sourceset
}

data class SingleplatformModuleIR(
    override val name: String,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val template: Template?,
    override val type: ModuleType,
    override val originalModule: Module,
    val sourcesets: List<SingleplatformSourcesetIR>
) : ModuleIR() {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SingleplatformModuleIR = copy(irs = irs)

    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> {
            indent(); sectionCall("dependencies", irsOfType<DependencyIR>())
        }
        is MavenPrinter -> node("dependencies") {
            irsOfType<DependencyIR>().listNl()
            sourcesets.forEach { sourceset ->
                sourceset.irsOfType<DependencyIR>().listNl()
            }
        }
        else -> Unit
    }
}

data class SingleplatformSourcesetIR(
    override val sourcesetType: SourcesetType,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val original: Sourceset
) : SourcesetIR, IrsOwner {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SingleplatformSourcesetIR = copy(irs = irs)
    override fun BuildFilePrinter.render() = Unit
}

data class SourcesetModuleIR(
    override val name: String,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val type: ModuleType,
    override val sourcesetType: SourcesetType,
    override val template: Template?,
    override val originalModule: Module,
    override val original: Sourceset
) : SourcesetIR, GradleIR, ModuleIR() {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SourcesetModuleIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() = getting(name, prefix = null) {
        val dependencies = irsOfType<DependencyIR>()
        val needBody = dependencies.isNotEmpty() || dsl == GradlePrinter.GradleDsl.GROOVY
        if (needBody) {
            +" "
            inBrackets {
                if (dependencies.isNotEmpty()) {
                    indent()
                    sectionCall("dependencies", dependencies)
                }
            }
        }
    }
}

val SourcesetModuleIR.targetName
    get() = name.removeSuffix(sourcesetType.name.capitalize())