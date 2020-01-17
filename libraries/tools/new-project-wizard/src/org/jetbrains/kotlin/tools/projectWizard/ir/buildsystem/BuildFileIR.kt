package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import java.nio.file.Path

data class BuildFileIR(
    val name: String,
    val directoryPath: Path,
    val modules: ModulesStructureIR,
    val pom: PomIR,
    override val irs: List<BuildSystemIR>
) : BuildSystemIR, IrsOwner {
    override fun withReplacedIrs(irs: List<BuildSystemIR>): BuildFileIR = copy(irs = irs)

    @Suppress("UNCHECKED_CAST")
    fun withModulesUpdated(updater: (ModuleIR) -> TaskResult<ModuleIR>): TaskResult<BuildFileIR> =
        modules.withModulesUpdated(updater).map { newModules -> copy(modules = newModules) }

    private fun distinctRepositories(): List<RepositoryIR> =
        irsOfType<RepositoryIR>()
            .distinctBy { it.repository }
            .sortedBy { repositoryIR ->
                if (repositoryIR.repository is DefaultRepository) 0 else 1
            }

    private fun distinctImportsOrNull(): List<GradleImportIR>? =
        irsOfTypeOrNull<GradleImportIR>()
            ?.distinctBy(GradleImportIR::import)
            ?.sortedBy(GradleImportIR::import)

    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> {
            distinctImportsOrNull()?.let { imports ->
                imports.listNl()
                nl()
                nl()
            }
            irsOfTypeOrNull<BuildScriptIR>()?.let { buildScriptIrs ->
                sectionCall("buildscript", needIndent = true) {
                    sectionCall("repositories", buildScriptIrs.filterIsInstance<BuildScriptRepositoryIR>())
                    nlIndented()
                    sectionCall("dependencies", buildScriptIrs.filterIsInstance<BuildScriptDependencyIR>())
                }
            }
            sectionCall("plugins", irsOfType<BuildSystemPluginIR>()); nlIndented()
            pom.render(this); nl()

            sectionCall("repositories", distinctRepositories())
            nl()
            modules.render(this)
            irsOfTypeOrNull<FreeIR>()?.let { freeIrs ->
                nl()
                freeIrs.listNl()
            }.ignore()
        }
        is MavenPrinter -> pom {
            pom.render(this)
            singleLineNode("packaging") { +"jar" }

            nl()
            singleLineNode("name") { +name }
            nl()

            node("properties") {
                singleLineNode("project.build.sourceEncoding") { +"UTF-8" }
                singleLineNode("kotlin.code.style") { +"official" }
            }

            distinctRepositories().takeIf { it.isNotEmpty() }?.let { repositories ->
                nl()
                node("repositories") {
                    repositories.listNl()
                }
            }

            val plugins = irsOfType<BuildSystemPluginIR>().takeIf { it.isNotEmpty() }

            if (plugins != null) {
                nl()
                node("build") {
                    singleLineNode("sourceDirectory") { +"src/main/kotlin" }
                    singleLineNode("testSourceDirectory") { +"src/test/kotlin" }
                    node("plugins") {
                        plugins.listNl()
                    }
                }
            }

            modules.render(this)
        }
        else -> Unit
    }
}

val BuildFileIR.targets
    get() = modules.safeAs<MultiplatformModulesStructureIR>()?.targets.orEmpty()

val BuildFileIR.sourcesets
    get() = modules.modules.flatMap { module ->
        when (module) {
            is SingleplatformModuleIR -> module.sourcesets
            is MultiplatformModuleIR -> listOf(module as SourcesetIR)
        }
    }

sealed class ModulesStructureIR : BuildSystemIR, IrsOwner {
    abstract val modules: List<ModuleIR>


    abstract fun withModules(modules: List<ModuleIR>): ModulesStructureIR
    fun withModulesUpdated(updater: (ModuleIR) -> TaskResult<ModuleIR>): TaskResult<ModulesStructureIR> =
        modules.map { updater(it) }
            .sequence()
            .map {
                withModules(it)
            }
}

data class MultiplatformModulesStructureIR(
    val targets: List<BuildSystemIR>,
    override val modules: List<MultiplatformModuleIR>,
    override val irs: List<BuildSystemIR>
) : GradleIR, ModulesStructureIR() {
    @Suppress("UNCHECKED_CAST")
    override fun withModules(modules: List<ModuleIR>) = copy(modules = modules as List<MultiplatformModuleIR>)

    override fun withReplacedIrs(irs: List<BuildSystemIR>): MultiplatformModulesStructureIR = copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        sectionCall("kotlin") {
            targets.filterNot {
                it.safeAs<DefaultTargetConfigurationIR>()?.targetAccess?.type == ModuleSubType.common
            }.listNl()
            nlIndented()
            sectionCall("sourceSets") {
                modules.listNl()
            }
        }
    }
}

fun MultiplatformModulesStructureIR.updateTargets(
    updater: (TargetConfigurationIR) -> TargetConfigurationIR
): MultiplatformModulesStructureIR {
    val newTargets = targets.map { target ->
        when (target) {
            is TargetConfigurationIR -> updater(target)
            else -> target
        }
    }
    return copy(targets = newTargets)
}

data class SingleplatformModulesStructureWithSingleModuleIR(
    val module: SingleplatformModuleIR,
    override val irs: List<BuildSystemIR>
) : ModulesStructureIR() {
    override val modules: List<SingleplatformModuleIR> = listOf(module)
    override fun withReplacedIrs(irs: List<BuildSystemIR>): SingleplatformModulesStructureWithSingleModuleIR =
        copy(irs = irs)

    @Suppress("UNCHECKED_CAST")
    override fun withModules(modules: List<ModuleIR>) =
        copy(module = modules.single() as SingleplatformModuleIR)

    override fun BuildFilePrinter.render() {
        module.render(this)
    }
}

data class RootFileModuleStructureIR(override val irs: List<BuildSystemIR>) : ModulesStructureIR() {
    override val modules = emptyList<ModuleIR>()

    override fun withReplacedIrs(irs: List<BuildSystemIR>): RootFileModuleStructureIR = copy(irs = irs)

    override fun BuildFilePrinter.render() = when (this) {
        is MavenPrinter -> {
            irs.listNl()
        }
        else -> Unit
    }

    override fun withModules(modules: List<ModuleIR>): RootFileModuleStructureIR = this
}