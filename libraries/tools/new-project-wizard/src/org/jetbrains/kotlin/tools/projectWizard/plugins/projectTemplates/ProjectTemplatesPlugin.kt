package org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.CustomMultiplatformProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate

class ProjectTemplatesPlugin(context: Context) : Plugin(context) {
    val template by dropDownSetting<ProjectTemplate>(
        "Template",
        GenerationPhase.INIT_TEMPLATE,
        valueParser { value, path ->
            CustomMultiplatformProjectTemplate
        }) {
        values = ProjectTemplate.ALL
    }

    val initTemplate by pipelineTask(GenerationPhase.INIT_TEMPLATE) {
        withAction {
            val selectedTemplate = ProjectTemplatesPlugin::template.settingValue
            selectedTemplate.setsValues.forEach { (setting, value) ->
                context.settingContext[setting] = value
            }
            UNIT_SUCCESS
        }
    }
}