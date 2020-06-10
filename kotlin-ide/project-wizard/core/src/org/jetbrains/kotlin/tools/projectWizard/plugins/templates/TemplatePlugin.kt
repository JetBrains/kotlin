package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

abstract class TemplatePlugin(context: Context) : Plugin(context) {
    fun addTemplateTask(template: Template) = pipelineTask(GenerationPhase.PREPARE) {
        withAction {
            TemplatesPlugin::addTemplate.execute(template)
        }
    }

    override val settings: List<PluginSetting<*, *>> = emptyList()
    override val pipelineTasks: List<PipelineTask> = emptyList()
}
