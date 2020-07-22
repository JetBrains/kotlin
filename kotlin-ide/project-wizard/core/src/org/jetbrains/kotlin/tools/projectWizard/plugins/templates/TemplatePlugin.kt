package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

abstract class TemplatePlugin(context: Context) : Plugin(context) {
    override val path = PATH

    override val settings: List<PluginSetting<*, *>> = emptyList()
    override val pipelineTasks: List<PipelineTask> = emptyList()
    override val properties: List<Property<*>> = emptyList()

    companion object {
        const val PATH = "template"

        fun addTemplateTask(prefix: String, template: Template) = pipelineTask(prefix, GenerationPhase.PREPARE) {
            withAction {
                TemplatesPlugin.addTemplate.execute(template)
            }
        }
    }
}
