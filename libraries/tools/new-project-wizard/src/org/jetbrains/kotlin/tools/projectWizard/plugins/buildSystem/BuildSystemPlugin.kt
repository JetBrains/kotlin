package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem


import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.BuildSystemAvailabilityWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.printBuildFile
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles

abstract class BuildSystemPlugin(context: Context) : Plugin(context) {
    val type by enumSetting<BuildSystemType>(
        KotlinNewProjectWizardBundle.message("plugin.buildsystem.setting.type"),
        GenerationPhase.FIRST_STEP
    ) {
        isSavable = true
        filter = { _, type ->
            val service = service<BuildSystemAvailabilityWizardService>()
            service.isAvailable(type)
        }

        validate { buildSystemType ->
            if (!buildSystemType.isGradle
                && KotlinPlugin::projectKind.reference.notRequiredSettingValue != ProjectKind.Singleplatform
            ) {
                val projectKind = KotlinPlugin::projectKind.reference.notRequiredSettingValue?.text?.capitalize()
                    ?: KotlinNewProjectWizardBundle.message("project")
                ValidationResult.ValidationError(
                    KotlinNewProjectWizardBundle.message(
                        "plugin.buildsystem.setting.type.error.wrong.project.kind",
                        projectKind,
                        buildSystemType.text
                    )
                )
            } else ValidationResult.OK
        }
    }

    val buildSystemData by property<List<BuildSystemData>>(emptyList())

    val buildFiles by listProperty<BuildFileIR>()

    val pluginRepositoreis by listProperty<Repository>()

    val takeRepositoriesFromDependencies by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(TemplatesPlugin::postApplyTemplatesToModules)

        withAction {
            updateBuildFiles { buildFile ->
                val dependenciesOfModule = buildList<LibraryDependencyIR> {
                    buildFile.modules.modules.forEach { module ->
                        if (module is SingleplatformModuleIR) module.sourcesets.forEach { sourceset ->
                            +sourceset.irs.filterIsInstance<LibraryDependencyIR>()
                        }
                        +module.irs.filterIsInstance<LibraryDependencyIR>()
                    }
                }
                val repositoriesToAdd = dependenciesOfModule.mapNotNull { dependency ->
                    dependency.artifact.safeAs<MavenArtifact>()?.repository?.let(::RepositoryIR)
                }
                buildFile.withIrs(repositoriesToAdd).asSuccess()
            }
        }
    }

    val createModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(StructurePlugin::createProjectDir)
        withAction {
            val fileSystem = service<FileSystemWizardService>()
            val data = BuildSystemPlugin::buildSystemData.propertyValue.first { it.type == buildSystemType }
            val buildFileData = data.buildFileData ?: return@withAction UNIT_SUCCESS
            BuildSystemPlugin::buildFiles.propertyValue.mapSequenceIgnore { buildFile ->
                fileSystem.createFile(
                    buildFile.directoryPath / buildFileData.buildFileName,
                    buildFileData.createPrinter().printBuildFile { buildFile.render(this) }
                )
            }
        }
    }

    val importProject by pipelineTask(GenerationPhase.PROJECT_IMPORT) {
        runAfter(BuildSystemPlugin::createModules)
        withAction {
            val data = BuildSystemPlugin::buildSystemData.propertyValue.first { it.type == buildSystemType }
            service<ProjectImportingWizardService> { service -> service.isSuitableFor(data.type) }
                .importProject(StructurePlugin::projectPath.reference.settingValue, allIRModules, buildSystemType)
        }
    }

    protected fun addBuildSystemData(data: BuildSystemData) = pipelineTask(GenerationPhase.PREPARE) {
        runBefore(BuildSystemPlugin::createModules)
        withAction {
            BuildSystemPlugin::buildSystemData.addValues(data)
        }
    }
}

data class BuildSystemData(
    val type: BuildSystemType,
    val buildFileData: BuildFileData?
)

data class BuildFileData(
    val createPrinter: () -> BuildFilePrinter,
    @NonNls val buildFileName: String
)

enum class BuildSystemType(@Nls override val text: String) : DisplayableSettingItem {
    GradleKotlinDsl(KotlinNewProjectWizardBundle.message("buildsystem.type.gradle.kotlin")),
    GradleGroovyDsl(KotlinNewProjectWizardBundle.message("buildsystem.type.gradle.groovy")),
    Jps(KotlinNewProjectWizardBundle.message("buildsystem.type.intellij")),
    Maven(KotlinNewProjectWizardBundle.message("buildsystem.type.maven"))

    ;

    override val greyText: String?
        get() = null
}

val BuildSystemType.isGradle
    get() = this == BuildSystemType.GradleGroovyDsl
            || this == BuildSystemType.GradleKotlinDsl

val Reader.allIRModules
    get() = BuildSystemPlugin::buildFiles.propertyValue.flatMap { buildFile ->
        buildFile.modules.modules
    }

val Writer.allModulesPaths
    get() = BuildSystemPlugin::buildFiles.propertyValue.flatMap { buildFile ->
        val paths = when (val structure = buildFile.modules) {
            is MultiplatformModulesStructureIR -> listOf(buildFile.directoryPath)
            else -> structure.modules.map { it.path }
        }
        paths.mapNotNull { path ->
            projectPath.relativize(path)
                .takeIf { it.toString().isNotBlank() }
                ?.toList()
                ?.takeIf { it.isNotEmpty() }
        }
    }


val Reader.buildSystemType: BuildSystemType
    get() = BuildSystemPlugin::type.reference.settingValue

