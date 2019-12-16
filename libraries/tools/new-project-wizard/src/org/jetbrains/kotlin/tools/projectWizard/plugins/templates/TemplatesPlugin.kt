package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.updateBuildFiles
import org.jetbrains.kotlin.tools.projectWizard.templates.*
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.TemplateInterceptor
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.fold

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
            val templateEngine = VelocityTemplateEngine()
            TemplatesPlugin::fileTemplatesToRender.propertyValue.mapSequenceIgnore { template ->
                with(templateEngine) { writeTemplate(template) }
            }
        }
    }

    val addTemplatesToSourcesets by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(KotlinPlugin::createModules)

        withAction {
            val templateEngine = VelocityTemplateEngine()
            updateBuildFiles { buildFile ->
                buildFile.modules.modules.mapSequence { module ->
                    when (module) {
                        is SingleplatformModuleIR -> {
                            module.sourcesets.mapSequence { sourceset ->
                                applyTemplateToSourceset(
                                    sourceset.template,
                                    sourceset,
                                    templateEngine
                                ).map { result ->
                                    result.copy(
                                        librariesToAdd = result.librariesToAdd.map {
                                            it.withDependencyType(sourceset.sourcesetType.toDependencyType())
                                        }
                                    )
                                }
                            }.map(List<TemplateApplicationResult>::fold)
                        }
                        is SourcesetModuleIR -> {
                            applyTemplateToSourceset(
                                module.template,
                                module,
                                templateEngine
                            )
                        }
                    }.map { result -> module.withIrs(result.librariesToAdd) to result }
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

    val postApplyTemplatesToSourcesets by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(BuildSystemPlugin::createModules)
        runAfter(KotlinPlugin::createModules)
        runAfter(TemplatesPlugin::addTemplatesToSourcesets)

        withAction {
            updateBuildFiles { buildFile ->
                @Suppress("UNCHECKED_CAST")
                val interceptors = buildFile.sourcesets.mapNotNull { sourceset ->
                    sourceset.template?.createInterceptors(sourceset) as? List<TemplateInterceptor>
                }.flatten().fold()
                interceptors.applyUntilConverge(buildFile).asSuccess()
            }
        }
    }
}