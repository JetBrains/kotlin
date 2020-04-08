package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PluginManagementRepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.RepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.SettingsGradleFileIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.printBuildFile
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor


abstract class GradlePlugin(context: Context) : BuildSystemPlugin(context) {
    val version by versionSetting("Gradle Version", GenerationPhase.FIRST_STEP) {
        defaultValue = value(defaultVersions.first())
        isAvailable = isGradle
    }

    val gradleVersions by property<List<Version>>(emptyList())

    val gradleProperties by listProperty(
        "kotlin.code.style" to "official"
    )

    val settingsGradleFileIRs by listProperty<BuildSystemIR>()

    val createGradlePropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runBefore(TemplatesPlugin::renderFileTemplates)
        isAvailable = isGradle
        withAction {
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/gradle.properties.vm",
                        "gradle.properties".asPath()
                    ),
                    StructurePlugin::projectPath.settingValue,
                    mapOf(
                        "properties" to GradlePlugin::gradleProperties.propertyValue
                    )
                )
            )
        }
    }

    val localProperties by listProperty<Pair<String, String>>()

    val createLocalPropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runBefore(TemplatesPlugin::renderFileTemplates)
        isAvailable = isGradle
        withAction {
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/local.properties.vm",
                        "local.properties".asPath()
                    ),
                    StructurePlugin::projectPath.settingValue,
                    mapOf(
                        "properties" to GradlePlugin::localProperties.propertyValue
                    )
                )
            )
        }
    }

    private val isGradle = checker { buildSystemType.isGradle }


    val initGradleWrapperTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(TemplatesPlugin::renderFileTemplates)
        isAvailable = isGradle
        withAction {
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/gradle-wrapper.properties.vm",
                        "gradle" / "wrapper" / "gradle-wrapper.properties"
                    ),
                    StructurePlugin::projectPath.reference.settingValue,
                    mapOf(
                        "version" to GradlePlugin::version.reference.settingValue.toString()
                    )
                )
            )
        }
    }


    val createSettingsFileTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runAfter(KotlinPlugin::createPluginRepositories)
        isAvailable = isGradle
        withAction {
            val (createBuildFile, buildFileName) = settingsGradleBuildFileData ?: return@withAction UNIT_SUCCESS

            val repositories = buildList<RepositoryIR> {
                +BuildSystemPlugin::pluginRepositoreis.propertyValue.map(::RepositoryIR)
                if (isNotEmpty()) {
                    +RepositoryIR(DefaultRepository.MAVEN_CENTRAL)
                }
            }.map(::PluginManagementRepositoryIR)

            val settingsGradleIR = SettingsGradleFileIR(
                StructurePlugin::name.settingValue,
                allModulesPaths.map { path -> path.joinToString(separator = "") { ":$it" } },
                buildPersistenceList {
                    +repositories
                    +GradlePlugin::settingsGradleFileIRs.propertyValue
                }
            )
            val buildFileText = createBuildFile().printBuildFile { settingsGradleIR.render(this) }
            service<FileSystemWizardService>().createFile(
                projectPath / buildFileName,
                buildFileText
            )
        }
    }

    companion object {
        // TODO update default versions
        private val defaultVersions = listOf("5.5.1").map(Version.Companion::fromString)
    }
}

val Reader.settingsGradleBuildFileData
    get() = when (buildSystemType) {
        BuildSystemType.GradleKotlinDsl ->
            BuildFileData(
                { GradlePrinter(GradlePrinter.GradleDsl.KOTLIN) },
                "settings.gradle.kts"
            )
        BuildSystemType.GradleGroovyDsl ->
            BuildFileData(
                { GradlePrinter(GradlePrinter.GradleDsl.GROOVY) },
                "settings.gradle"
            )
        else -> null
    }