package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.fold
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.service.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildFileIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.RepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.withIrs
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.pomIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Path

class KotlinPlugin(context: Context) : Plugin(context) {
    val version by property(
        // todo do not hardcode kind & repository
        WizardKotlinVersion(Versions.KOTLIN, KotlinVersionKind.M, Repositories.KOTLIN_EAP_BINTRAY)
    )

    val initKotlinVersions by pipelineTask(GenerationPhase.PREPARE_GENERATION) {
        title = KotlinNewProjectWizardBundle.message("plugin.kotlin.downloading.kotlin.versions")

        withAction {
            val version = service<KotlinVersionProviderService>().getKotlinVersion()
            KotlinPlugin::version.update { version.asSuccess() }
        }
    }

    val projectKind by enumSetting<ProjectKind>(
        KotlinNewProjectWizardBundle.message("plugin.kotlin.setting.project.kind"),
        GenerationPhase.FIRST_STEP
    )

    private fun List<Module>.findDuplicatesByName() =
        groupingBy { it.name }.eachCount().filter { it.value > 1 }

    val modules by listSetting(
        KotlinNewProjectWizardBundle.message("plugin.kotlin.setting.modules"),
        GenerationPhase.SECOND_STEP,
        Module.parser
    ) {
        validate { value ->
            val allModules = value.withAllSubModules()
            val duplicatedModules = allModules.findDuplicatesByName()
            if (duplicatedModules.isEmpty()) ValidationResult.OK
            else ValidationResult.ValidationError(
                KotlinNewProjectWizardBundle.message(
                    "plugin.kotlin.setting.modules.error.duplicated.modules",
                    duplicatedModules.values.first(),
                    duplicatedModules.keys.first()
                )
            )
        }

        validate { value ->
            value.withAllSubModules().filter { it.kind == ModuleKind.multiplatform }.map { module ->
                val duplicatedModules = module.subModules.findDuplicatesByName()
                if (duplicatedModules.isEmpty()) ValidationResult.OK
                else ValidationResult.ValidationError(
                    KotlinNewProjectWizardBundle.message(
                        "plugin.kotlin.setting.modules.error.duplicated.targets",
                        duplicatedModules.values.first(),
                        duplicatedModules.keys.first()
                    )
                )
            }.fold()
        }

        validate(moduleDependenciesValidator)
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
            val version = KotlinPlugin::version.propertyValue
            if (version.kind.isStable) return@withAction UNIT_SUCCESS
            val pluginRepository = version.repository
            BuildSystemPlugin::pluginRepositoreis.addValues(pluginRepository) andThen
                    updateBuildFiles { buildFile ->
                        buildFile.withIrs(RepositoryIR(pluginRepository)).asSuccess()
                    }
        }
    }

    val createSourcesetDirectories by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        withAction {
            fun Path.createKotlinAndResourceDirectories(moduleConfigurator: ModuleConfigurator) = with(service<FileSystemWizardService>()) {
                createDirectory(this@createKotlinAndResourceDirectories / moduleConfigurator.kotlinDirectoryName) andThen
                        createDirectory(this@createKotlinAndResourceDirectories / moduleConfigurator.resourcesDirectoryName)
            }

            forEachModule { moduleIR ->
                moduleIR.sourcesets.mapSequenceIgnore { sourcesetIR ->
                    sourcesetIR.path.createKotlinAndResourceDirectories(moduleIR.originalModule.configurator)
                }
            }
        }
    }


    private fun Writer.createBuildFiles(modules: List<Module>): TaskResult<List<BuildFileIR>> =
        with(
            ModulesToIRsConverter(
                ModulesToIrConversionData(
                    modules,
                    projectPath,
                    StructurePlugin::name.settingValue,
                    KotlinPlugin::version.propertyValue,
                    buildSystemType,
                    pomIR()
                )
            )
        ) { createBuildFiles() }


    companion object {
        private val moduleDependenciesValidator = settingValidator<List<Module>> { modules ->
            val allModules = modules.withAllSubModules(includeSourcesets = true).toSet()
            val allModulePaths = allModules.map(Module::path).toSet()
            allModules.flatMap { module ->
                module.dependencies.map { dependency ->
                    val isValidModule = when (dependency) {
                        is ModuleReference.ByPath -> dependency.path in allModulePaths
                        is ModuleReference.ByModule -> dependency.module in allModules
                    }
                    ValidationResult.create(isValidModule) {
                        KotlinNewProjectWizardBundle.message(
                            "plugin.kotlin.setting.modules.error.duplicated.modules",
                            dependency,
                            module.path
                        )
                    }
                }
            }.fold()
        }
    }
}

enum class ProjectKind(override val text: String) : DisplayableSettingItem {
    Singleplatform(KotlinNewProjectWizardBundle.message("project.kind.singleplatform")),
    Multiplatform(KotlinNewProjectWizardBundle.message("project.kind.multiplatform")),
    Android(KotlinNewProjectWizardBundle.message("project.kind.android")),
    Js(KotlinNewProjectWizardBundle.message("project.kind.kotlin.js"))
}

fun List<Module>.withAllSubModules(includeSourcesets: Boolean = false): List<Module> = buildList {
    fun handleModule(module: Module) {
        +module
        if (module.kind != ModuleKind.multiplatform
            || includeSourcesets && module.kind == ModuleKind.multiplatform
        ) {
            module.subModules.forEach(::handleModule)
        }
    }
    forEach(::handleModule)
}