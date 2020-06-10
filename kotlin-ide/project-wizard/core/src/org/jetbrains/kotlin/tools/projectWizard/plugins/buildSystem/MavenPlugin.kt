package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.asSuccess
import org.jetbrains.kotlin.tools.projectWizard.core.checker
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.RootFileModuleStructureIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.ModulesDependencyMavenIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.maven.PluginRepositoryMavenIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.withIrs
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles

class MavenPlugin(context: Context) : BuildSystemPlugin(context) {
    override val path = "buildSystem.maven"

    private val isMaven = checker {
        BuildSystemPlugin::type.settingValue == BuildSystemType.Maven
    }

    val createSettingsFileTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runBefore(BuildSystemPlugin::createModules)
        isAvailable = isMaven
        withAction {
            BuildSystemPlugin::buildFiles.update { buildFiles ->
                if (buildFiles.size == 1) return@update buildFiles.asSuccess()
                buildFiles.map { buildFile ->
                    when (val structure = buildFile.modules) {
                        is RootFileModuleStructureIR -> {
                            val dependencies = allModulesPaths.map { path ->
                                path.joinToString(separator = "/")
                            }
                            buildFile.copy(modules = structure.withIrs(ModulesDependencyMavenIR(dependencies)))
                        }
                        else -> buildFile
                    }
                }.asSuccess()
            }
        }
    }

    val addBuildSystemPluginRepositories by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createPluginRepositories)
        runBefore(BuildSystemPlugin::createModules)
        isAvailable = isMaven

        withAction {
            val repositories = BuildSystemPlugin::pluginRepositoreis.propertyValue
            updateBuildFiles { buildFile ->
                buildFile.withIrs(repositories.map(::PluginRepositoryMavenIR)).asSuccess()
            }
        }
    }

    val addBuildSystemData by addBuildSystemData(
        BuildSystemData(
            type = BuildSystemType.Maven,
            buildFileData = BuildFileData(
                createPrinter = { MavenPrinter() },
                buildFileName = "pom.xml"
            )
        )
    )

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                createSettingsFileTask,
                addBuildSystemPluginRepositories,
                addBuildSystemData
            )
}