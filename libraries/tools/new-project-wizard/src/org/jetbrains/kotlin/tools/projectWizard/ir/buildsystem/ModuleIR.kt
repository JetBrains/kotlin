package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
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
    abstract val sourcesets: List<SourcesetIR>
}


data class SingleplatformModuleIR(
    override val name: String,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val template: Template?,
    override val type: ModuleType,
    override val originalModule: Module,
    override val sourcesets: List<SingleplatformSourcesetIR>
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


data class MultiplatformModuleIR(
    override val name: String,
    override val path: Path,
    override val irs: List<BuildSystemIR>,
    override val type: ModuleType,
    override val template: Template?,
    override val originalModule: Module,
    override val sourcesets: List<MultiplatformSourcesetIR>
) : GradleIR, ModuleIR() {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): MultiplatformModuleIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        sourcesets.map { sourceset ->
            sourceset.withIrs(
                irsOfType<DependencyIR>().filter { dependency ->
                    dependency.dependencyType == sourceset.sourcesetType.toDependencyType()
                }
            )
        }.listNl(needFirstIndent = false)
    }
}

fun MultiplatformModuleIR.neighbourTargetModules() =
    originalModule.parent
        ?.takeIf { it.kind == ModuleKind.multiplatform }
        ?.subModules
        .orEmpty()