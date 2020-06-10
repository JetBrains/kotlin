package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleJsClientTemplate

class JsTemplatesPlugin(context: Context) : TemplatePlugin(context) {
    val addTemplate by addTemplateTask(SimpleJsClientTemplate())

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                addTemplate,
            )
}