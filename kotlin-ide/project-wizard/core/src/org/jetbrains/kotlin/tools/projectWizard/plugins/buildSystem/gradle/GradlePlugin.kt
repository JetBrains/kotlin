package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle


import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.SettingsGradleFileIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.printBuildFile
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateModules
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor


abstract class GradlePlugin(context: Context) : BuildSystemPlugin(context) {
    override val path = pluginPath

    companion object : PluginSettingsOwner() {
        override val pluginPath = "buildSystem.gradle"

        val gradleProperties by listProperty(

            "kotlin.code.style" to "official"
        )

        val settingsGradleFileIRs by listProperty<BuildSystemIR>()

        val createGradlePropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
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

        val localProperties by listProperty<Pair<String, String>>()

        val createLocalPropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            runBefore(TemplatesPlugin.renderFileTemplates)
            isAvailable = isGradle
            withAction {
                val properties = localProperties.propertyValue
                if (properties.isEmpty()) return@withAction UNIT_SUCCESS
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

        val initGradleWrapperTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
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

        val mergeCommonRepositories by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
            runBefore(createModules)
            runAfter(takeRepositoriesFromDependencies)
            runAfter(KotlinPlugin.createPluginRepositories)

            isAvailable = isGradle

            withAction {
                val buildFiles = buildFiles.propertyValue
                if (buildFiles.size == 1) return@withAction UNIT_SUCCESS
                val moduleRepositories = buildFiles.mapNotNull { buildFileIR ->
                    if (buildFileIR.isRoot) null
                    else buildFileIR.irs.mapNotNull { it.safeAs<RepositoryIR>()?.repository }
                }

                val allRepositories = moduleRepositories.flatMapTo(hashSetOf()) { it }

                val commonRepositories = allRepositories.filterTo(
                    hashSetOf(KotlinPlugin.version.propertyValue.repository)
                ) { repo ->
                    moduleRepositories.all { repo in it }
                }

                updateBuildFiles { buildFile ->
                    buildFile.withReplacedIrs(
                        buildFile.irs
                            .filterNot { it.safeAs<RepositoryIR>()?.repository in commonRepositories }
                            .toPersistentList()
                    ).let {
                        if (it.isRoot && commonRepositories.isNotEmpty()) {
                            val repositories = commonRepositories.map(::RepositoryIR).distinctAndSorted()
                            it.withIrs(AllProjectsRepositoriesIR(repositories))
                        } else it
                    }.asSuccess()
                }
            }
        }


        val createSettingsFileTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
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
                mergeCommonRepositories,
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