package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderServiceImpl
import org.jetbrains.kotlin.tools.projectWizard.core.service.kotlinVersionKind
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Path

class KotlinPlugin(context: Context) : Plugin(context) {
    val version by property(KotlinVersionProviderServiceImpl.DEFAULT)

    val initKotlinVersions by pipelineTask(GenerationPhase.PREPARE) {
        title = "Downloading list of Kotlin versions"

        withAction {
            val version = service<KotlinVersionProviderService>()!!.getKotlinVersion()
            KotlinPlugin::version.update { version.asSuccess() }
        }
    }

    val projectKind by enumSetting<ProjectKind>("Project Kind", GenerationPhase.FIRST_STEP)

    private fun List<Module>.findDuplicatesByName() =
        groupingBy { it.name }.eachCount().filter { it.value > 1 }

    val modules by listSetting("Modules", GenerationPhase.SECOND_STEP, Module.parser) {
        validate { value ->
            val allModules = value.withAllSubModules()
            val duplicatedModules = allModules.findDuplicatesByName()
            if (duplicatedModules.isEmpty()) ValidationResult.OK
            else ValidationResult.ValidationError(
                "There are ${duplicatedModules.values.first()} modules with name `${duplicatedModules.keys.first()}`"
            )
        }

        validate { value ->
            value.withAllSubModules().filter { it.kind == ModuleKind.multiplatform }.map { module ->
                val duplicatedModules = module.subModules.findDuplicatesByName()
                if (duplicatedModules.isEmpty()) ValidationResult.OK
                else ValidationResult.ValidationError(
                    "There are ${duplicatedModules.values.first()} targets for module `${module.name}` with name `${duplicatedModules.keys.first()}`"
                )
            }.fold()
        }
    }


    val createModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(StructurePlugin::createProjectDir)
        withAction {
            BuildSystemPlugin::buildFiles.update {
                val modules = KotlinPlugin::modules.settingValue
                val (buildFiles) = createBuildFiles(modules)
                buildFiles.map { it.withIrs(RepositoryIR(DefaultRepository.MAVEN_CENTRAL)) }.asSuccess()
            }
        }
    }

    val createPluginRepositories by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        withAction {
            val pluginRepository = KotlinPlugin::version.propertyValue.kotlinVersionKind.repository ?: return@withAction UNIT_SUCCESS
            BuildSystemPlugin::pluginRepositoreis.addValues(pluginRepository) andThen
                    updateBuildFiles { buildFile ->
                        buildFile.withIrs(RepositoryIR(pluginRepository)).asSuccess()
                    }
        }
    }

    val createSourcesetDirectories by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        withAction {
            fun Path.createKotlinAndResourceDirectories() = with(service<FileSystemWizardService>()!!) {
                createDirectory(this@createKotlinAndResourceDirectories / Defaults.KOTLIN_DIR) andThen
                        createDirectory(this@createKotlinAndResourceDirectories / Defaults.RESOURCES_DIR)
            }

            forEachModule { moduleIR ->
                moduleIR.sourcesets.mapSequenceIgnore { sourcesetIR ->
                    sourcesetIR.path.createKotlinAndResourceDirectories()
                }
            }
        }
    }


    private fun TaskRunningContext.createBuildFiles(modules: List<Module>): TaskResult<List<BuildFileIR>> =
        with(
            ModulesToIRsConverter(
                ModuleConfigurationData(
                    modules,
                    projectPath,
                    StructurePlugin::name.settingValue,
                    KotlinPlugin::version.propertyValue,
                    buildSystemType,
                    pomIR(),
                    this
                )
            )
        ) { createBuildFiles() }


    companion object {
        // TODO update default versions
        private val DEFAULT_VERSION = Version.fromString("1.3.61")

    }
}

enum class ProjectKind(override val text: String) : DisplayableSettingItem {
    Singleplatform("Singleplatform JVM project"),
    Multiplatform("Multiplatform project")
}

fun List<Module>.withAllSubModules(includeSourcesets: Boolean = false): List<Module> = buildList {
    fun handleModule(module: Module) {
        +module
        if (module.kind == ModuleKind.singleplatformJvm
            || includeSourcesets && module.kind == ModuleKind.multiplatform
        ) {
            module.subModules.forEach(::handleModule)
        }
    }
    forEach(::handleModule)
}
