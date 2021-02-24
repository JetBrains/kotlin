package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.PluginSettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleJsClientTemplate

class JsTemplatesPlugin(context: Context) : TemplatePlugin(context) {
    override val path = pluginPath

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                addTemplate,
            )

    companion object : PluginSettingsOwner() {
        override val pluginPath = "template.jsTemplates"

        val addTemplate by pipelineTask(GenerationPhase.PREPARE) {
            withAction {
                TemplatesPlugin.addTemplate.execute(SimpleJsClientTemplate())
            }
        }
    }
}