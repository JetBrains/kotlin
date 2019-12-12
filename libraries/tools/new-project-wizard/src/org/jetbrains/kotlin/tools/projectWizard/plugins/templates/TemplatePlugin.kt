package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase

abstract class TemplatePlugin(context: Context) : Plugin(context) {
    fun addTemplateTask(template: Template) = pipelineTask(GenerationPhase.PREPARE) {
        withAction {
            TemplatesPlugin::addTemplate.execute(template)
        }
    }
}
