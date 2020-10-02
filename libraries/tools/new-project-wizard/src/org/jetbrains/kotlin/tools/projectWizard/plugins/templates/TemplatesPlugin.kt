package org.jetbrains.kotlin.tools.projectWizard.plugins.templates


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.Defaults.SRC_DIR
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.service.TemplateEngineService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfiguratorWithTests
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.MppModuleConfigurator.runArbitraryTask
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.isPresent
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectName
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles
import org.jetbrains.kotlin.tools.projectWizard.templates.*
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.InterceptionPoint
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptionApplicationState
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.applyAll
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.settingValue
import java.nio.file.Path

class TemplatesPlugin(context: Context) : Plugin(context) {
    override val path = pluginPath

    companion object : PluginSettingsOwner() {
        override val pluginPath = "templates"

        val templates by property<Map<String, Template>>(emptyMap())

        val addTemplate by task1<Template, Unit> {
            withAction { template ->
                templates.update { success(it + (template.id to template)) }
            }
        }

        val fileTemplatesToRender by property<List<FileTemplate>>(emptyList())

        val addFileTemplate by task1<FileTemplate, Unit> {
            withAction { template ->
                fileTemplatesToRender.update { success(it + template) }
            }
        }

        val addFileTemplates by task1<List<FileTemplate>, Unit> {
            withAction { templates ->
                fileTemplatesToRender.addValues(templates)
            }
        }

        val renderFileTemplates by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            withAction {
                val templateEngine = service<TemplateEngineService>()
                fileTemplatesToRender.propertyValue.mapSequenceIgnore { template ->
                    with(templateEngine) { writeTemplate(template) }
                }
            }
        }

        val addTemplatesToModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
            runBefore(BuildSystemPlugin.createModules)
            runAfter(KotlinPlugin.createModules)

            withAction {
                updateBuildFiles { buildFile ->
                    val moduleStructure = buildFile.modules
                    val moduleIRs = buildList<ModuleIR> {
                        if (moduleStructure is MultiplatformModulesStructureIR) {
                            +moduleStructure.multiplatformModule
                        }
                        +moduleStructure.modules
                    }
                    moduleIRs.mapSequence { module ->
                        applyTemplateToModule(
                            module.template,
                            module
                        ).map { result -> result.updateModuleIR(module.withIrs(result.librariesToAdd)) to result }
                    }.map {
                        val (moduleIrs, results) = it.unzip()
                        val foldedResults = results.fold()
                        buildFile.copy(
                            modules = buildFile.modules.withModules(moduleIrs.filterNot { it is FakeMultiplatformModuleIR })
                        ).withIrs(foldedResults.irsToAddToBuildFile).let { buildFile ->
                            when (val structure = buildFile.modules) {
                                is MultiplatformModulesStructureIR ->
                                    buildFile.copy(
                                        modules = structure
                                            .updateTargets(foldedResults.updateTarget)
                                            .updateSourceSets(foldedResults.updateModuleIR)
                                    )
                                else -> buildFile
                            }
                        }
                    }
                }
            }
        }

        val postApplyTemplatesToModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
            runBefore(BuildSystemPlugin.createModules)
            runAfter(KotlinPlugin.createModules)
            runAfter(TemplatesPlugin.addTemplatesToModules)

            withAction {
                updateBuildFiles { buildFile ->
                    val modules = buildFile.modules.modules

                    val applicationState = modules.mapNotNull { module ->
                        module.template?.createInterceptors(module)
                    }.flatten()
                        .applyAll(TemplateInterceptionApplicationState(buildFile, emptyMap()))

                    val templateEngine = service<TemplateEngineService>()

                    val templatesApplicationResult = modules.map { module ->
                        val settings = applicationState.moduleToSettings[module.originalModule.identificator].orEmpty()
                        applyFileTemplatesFromSourceset(module, templateEngine, settings)
                    }.sequenceIgnore()

                    templatesApplicationResult andThen applicationState.buildFileIR.asSuccess()
                }
            }
        }

        private fun Writer.applyFileTemplatesFromSourceset(
            module: ModuleIR,
            templateEngine: TemplateEngineService,
            interceptionPointSettings: Map<InterceptionPoint<Any>, Any>
        ): TaskResult<Unit> {
            val template = module.template ?: return UNIT_SUCCESS
            val settings = with(template) { settingsAsMap(module.originalModule) }
            val allSettings: Map<String, Any> = mutableMapOf<String, Any>().apply {
                putAll(settings)
                putAll(interceptionPointSettings.mapKeys { it.key.name })
                putAll(defaultSettings(module))
            }
            return with(template) { getFileTemplates(module) }.mapNotNull { (fileTemplateDescriptor, filePath) ->
                val path = generatePathForFileTemplate(module, filePath) ?: return@mapNotNull null
                val fileTemplate = FileTemplate(
                    fileTemplateDescriptor,
                    module.path / path,
                    allSettings
                )
                with(templateEngine) { writeTemplate(fileTemplate) }
            }.sequenceIgnore()
        }

        private fun Reader.defaultSettings(moduleIR: ModuleIR) = mapOf(
            "projectName" to projectName,
            "moduleName" to moduleIR.name
        )

        private fun Reader.generatePathForFileTemplate(module: ModuleIR, filePath: FilePath): Path? {
            if (filePath is SrcFilePath
                && filePath.sourcesetType == SourcesetType.test
                && settingValue(module.originalModule, ModuleConfiguratorWithTests.testFramework)?.isPresent != true
            ) return null
            val moduleConfigurator = module.originalModule.configurator
            return when (module) {
                is SingleplatformModuleIR -> {
                    when (filePath) {
                        is SrcFilePath -> SRC_DIR / filePath.sourcesetType.toString() / moduleConfigurator.kotlinDirectoryName
                        is ResourcesFilePath -> SRC_DIR / filePath.sourcesetType.toString() / moduleConfigurator.resourcesDirectoryName
                    }
                }

                is MultiplatformModuleIR -> {
                    val directory = when (filePath) {
                        is SrcFilePath -> moduleConfigurator.kotlinDirectoryName
                        is ResourcesFilePath -> moduleConfigurator.resourcesDirectoryName
                    }
                    SRC_DIR / "${module.name}${filePath.sourcesetType.name.capitalize()}" / directory
                }
                is FakeMultiplatformModuleIR -> error("Not supported for FakeMultiplatformModuleIR")
            }
        }
    }

    override val settings: List<PluginSetting<*, *>> = listOf()
    override val pipelineTasks: List<PipelineTask> =
        listOf(
            renderFileTemplates,
            addTemplatesToModules,
            postApplyTemplatesToModules
        )
    override val properties: List<Property<*>> =
        listOf(
            templates,
            fileTemplatesToRender
        )
}