package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.templates.ConsoleJvmApplicationTemplate

class ConsoleJvmApplicationTemplatePlugin(context: Context) : TemplatePlugin(context) {
    override val path = PATH

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks + listOf(addTemplate)

    companion object {
        private const val PATH = "template.consoleJvmApplicationTemplate"

        val addTemplate by addTemplateTask(PATH, ConsoleJvmApplicationTemplate())
    }
}