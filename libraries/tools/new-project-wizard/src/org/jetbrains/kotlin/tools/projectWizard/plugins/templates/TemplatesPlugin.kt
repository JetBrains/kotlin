package org.jetbrains.kotlin.tools.projectWizard.plugins.templates


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.Defaults.KOTLIN_DIR
import org.jetbrains.kotlin.tools.projectWizard.core.Defaults.RESOURCES_DIR
import org.jetbrains.kotlin.tools.projectWizard.core.Defaults.SRC_DIR
import org.jetbrains.kotlin.tools.projectWizard.core.service.TemplateEngineService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectName
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles
import org.jetbrains.kotlin.tools.projectWizard.templates.*
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.InterceptionPoint
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptionApplicationState
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.applyAll

class TemplatesPlugin(context: Context) : Plugin(context) {
    val templates by property<Map<String, Template>>(
        emptyMap()
    )

    val addTemplate by task1<Template, Unit> {
        withAction { template ->
            TemplatesPlugin::templates.update { success(it + (template.id to template)) }
        }
    }

    val fileTemplatesToRender by property<List<FileTemplate>>(emptyList())

    val addFileTemplate by task1<FileTemplate, Unit> {
        withAction { template ->
            TemplatesPlugin::fileTemplatesToRender.update { success(it + template) }
        }
    }

    val addFileTemplates by task1<List<FileTemplate>, Unit> {
        withAction { templates ->
            TemplatesPlugin::fileTemplatesToRender.addValues(templates)
        }
    }

    val renderFileTemplates by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        withAction {
            val templateEngine = service<TemplateEngineService>()
            TemplatesPlugin::fileTemplatesToRender.propertyValue.mapSequenceIgnore { template ->
                with(templateEngine) { writeTemplate(template) }
            }
        }
    }

    val addTemplatesToModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(KotlinPlugin::createModules)

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

    val postApplyTemplatesToModules by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(KotlinPlugin::createModules)
        runAfter(TemplatesPlugin::addTemplatesToModules)

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
        return with(template) { getFileTemplates(module) }.map { (fileTemplateDescriptor, filePath) ->
            val path = generatePathForFileTemplate(module, filePath)
            val fileTemplate = FileTemplate(
                fileTemplateDescriptor,
                module.path / path,
                allSettings
            )
            with(templateEngine) { writeTemplate(fileTemplate) }
        }.sequenceIgnore()
    }

    private fun Writer.defaultSettings(moduleIR: ModuleIR) = mapOf(
        "projectName" to projectName,
        "moduleName" to moduleIR.name
    )

    private fun generatePathForFileTemplate(module: ModuleIR, filePath: FilePath) = when (module) {
        is SingleplatformModuleIR -> {
            when (filePath) {
                is SrcFilePath -> SRC_DIR / filePath.sourcesetType.toString() / KOTLIN_DIR
                is ResourcesFilePath -> SRC_DIR / filePath.sourcesetType.toString() / RESOURCES_DIR
            }
        }

        is MultiplatformModuleIR -> {
            val directory = when (filePath) {
                is SrcFilePath -> KOTLIN_DIR
                is ResourcesFilePath -> RESOURCES_DIR
            }
            SRC_DIR / "${module.name}${filePath.sourcesetType.name.capitalize()}" / directory
        }
    }
}