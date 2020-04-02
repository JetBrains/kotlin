package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Path

sealed class ModuleIR : IrsOwner, BuildSystemIR {
    abstract val name: String
    abstract val path: Path
    abstract val template: Template?
    abstract val originalModule: Module
    abstract val sourcesets: List<SourcesetIR>
}


data class SingleplatformModuleIR(
    override val name: String,
    override val path: Path,
    override val irs: PersistentList<BuildSystemIR>,
    override val template: Template?,
    override val originalModule: Module,
    override val sourcesets: List<SingleplatformSourcesetIR>
) : ModuleIR() {
    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): SingleplatformModuleIR = copy(irs = irs)

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
    override val irs: PersistentList<BuildSystemIR>,
    override val template: Template?,
    override val originalModule: Module,
    override val sourcesets: List<MultiplatformSourcesetIR>
) : GradleIR, ModuleIR() {
    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): MultiplatformModuleIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        sourcesets.map { sourceset ->
            sourceset.withIrs(
                irsOfType<DependencyIR>().filter { dependency ->
                    dependency.dependencyType == sourceset.sourcesetType.toDependencyType()
                }.map { it.withDependencyType(DependencyType.MAIN) }
            )
        }.listNl(needFirstIndent = false)
    }
}

fun MultiplatformModuleIR.neighbourTargetModules() =
    originalModule.parent
        ?.takeIf { it.kind == ModuleKind.multiplatform }
        ?.subModules
        .orEmpty()