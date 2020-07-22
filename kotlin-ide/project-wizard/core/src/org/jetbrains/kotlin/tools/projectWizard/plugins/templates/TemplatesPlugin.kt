package org.jetbrains.kotlin.tools.projectWizard.plugins.templates


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.Defaults.SRC_DIR
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.service.TemplateEngineService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfiguratorWithTests
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
    override val path = PATH

    companion object {
        private const val PATH = "templates"

        val templates by property<Map<String, Template>>(
            PATH,
            emptyMap()
        )

        val addTemplate by task1<Template, Unit>(PATH) {
            withAction { template ->
                templates.update { success(it + (template.id to template)) }
            }
        }

        val fileTemplatesToRender by property<List<FileTemplate>>(PATH, emptyList())

        val addFileTemplate by task1<FileTemplate, Unit>(PATH) {
            withAction { template ->
                fileTemplatesToRender.update { success(it + template) }
            }
        }

        val addFileTemplates by task1<List<FileTemplate>, Unit>(PATH) {
            withAction { templates ->
                fileTemplatesToRender.addValues(templates)
            }
        }

        val renderFileTemplates by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runAfter(KotlinPlugin.createModules)
            withAction {
                val templateEngine = service<TemplateEngineService>()
                fileTemplatesToRender.propertyValue.mapSequenceIgnore { template ->
                    with(templateEngine) { writeTemplate(template) }
                }
            }
        }

        val addTemplatesToModules by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
            runBefore(BuildSystemPlugin.createModules)
            runAfter(KotlinPlugin.createModules)

            withAction {
                updateBuildFiles { buildFile ->
                    buildFile.modules.modules.mapSequence { module ->
                        applyTemplateToModule(
                            module.template,
                            module
                        ).map { result -> module.withIrs(result.librariesToAdd) to result }
                    }.map {
                        val (moduleIrs, results) = it.unzip()
                        val foldedResults = results.fold()
                        buildFile.copy(
                            modules = buildFile.modules.withModules(moduleIrs)
                        ).withIrs(foldedResults.irsToAddToBuildFile).let { buildFile ->
                            when (val structure = buildFile.modules) {
                                is MultiplatformModulesStructureIR ->
                                    buildFile.copy(modules = structure.updateTargets(foldedResults.updateTarget))
                                else -> buildFile
                            }
                        }
                    }
                }
            }
        }

        val postApplyTemplatesToModules by pipelineTask(PATH, GenerationPhase.PROJECT_GENERATION) {
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