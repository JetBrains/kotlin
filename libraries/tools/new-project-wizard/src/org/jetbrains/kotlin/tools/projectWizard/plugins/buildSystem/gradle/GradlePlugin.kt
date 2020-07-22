package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle


import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
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
    override val path = PATH

    companion object {
        private const val PATH = "buildSystem.gradle"

        val gradleProperties by listProperty(
            PATH,
            "kotlin.code.style" to "official"
        )

        val settingsGradleFileIRs by listProperty<BuildSystemIR>(PATH)

        val createGradlePropertiesFile by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            runBefore(TemplatesPlugin.renderFileTemplates)
            isAvailable = isGradle
            withAction {
                TemplatesPlugin.addFileTemplate.execute(
                    FileTemplate(
                        FileTemplateDescriptor(
                            "gradle/gradle.properties.vm",
                            "gradle.properties".asPath()
                        ),
                        StructurePlugin.projectPath.settingValue,
                        mapOf(
                            "properties" to gradleProperties
                                .propertyValue
                                .distinctBy { it.first }
                        )
                    )
                )
            }
        }

        val localProperties by listProperty<Pair<String, String>>(PATH)

        val createLocalPropertiesFile by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            runBefore(TemplatesPlugin.renderFileTemplates)
            isAvailable = isGradle
            withAction {
                TemplatesPlugin.addFileTemplate.execute(
                    FileTemplate(
                        FileTemplateDescriptor(
                            "gradle/local.properties.vm",
                            "local.properties".asPath()
                        ),
                        StructurePlugin.projectPath.settingValue,
                        mapOf(
                            "properties" to localProperties.propertyValue
                        )
                    )
                )
            }
        }

        private val isGradle = checker { buildSystemType.isGradle }

        val initGradleWrapperTask by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runBefore(TemplatesPlugin.renderFileTemplates)
            isAvailable = isGradle
            withAction {
                TemplatesPlugin.addFileTemplate.execute(
                    FileTemplate(
                        FileTemplateDescriptor(
                            "gradle/gradle-wrapper.properties.vm",
                            "gradle" / "wrapper" / "gradle-wrapper.properties"
                        ),
                        StructurePlugin.projectPath.settingValue,
                        mapOf(
                            "version" to Versions.GRADLE
                        )
                    )
                )
            }
        }


        val createSettingsFileTask by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            runAfter(KotlinPlugin.createPluginRepositories)
            isAvailable = isGradle
            withAction {
                val (createBuildFile, buildFileName) = settingsGradleBuildFileData ?: return@withAction UNIT_SUCCESS

                val repositories = buildList<RepositoryIR> {
                    +pluginRepositoreis.propertyValue.map(::RepositoryIR)
                    if (isNotEmpty()) {
                        +RepositoryIR(DefaultRepository.MAVEN_CENTRAL)
                        +RepositoryIR(DefaultRepository.GRADLE_PLUGIN_PORTAL)
                    }
                }.map(::PluginManagementRepositoryIR)

                val settingsGradleIR = SettingsGradleFileIR(
                    StructurePlugin.name.settingValue,
                    allModulesPaths.map { path -> path.joinToString(separator = "") { ":$it" } },
                    buildPersistenceList {
                        +repositories
                        +settingsGradleFileIRs.propertyValue
                    }
                )
                val buildFileText = createBuildFile().printBuildFile { settingsGradleIR.render(this) }
                service<FileSystemWizardService>().createFile(
                    projectPath / buildFileName,
                    buildFileText
                )
            }
        }
    }

    override val settings: List<PluginSetting<*, *>> = super.settings
    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                createGradlePropertiesFile,
                createLocalPropertiesFile,
                initGradleWrapperTask,
                createSettingsFileTask,
            )
    override val properties: List<Property<*>> = super.properties +
            listOf(
                gradleProperties,
                settingsGradleFileIRs,
                localProperties
            )
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