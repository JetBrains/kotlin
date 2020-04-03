package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.templates.ConsoleJvmApplicationTemplate

class ConsoleJvmApplicationTemplatePlugin(context: Context) : TemplatePlugin(context) {
    val addTemplate by addTemplateTask(
        ConsoleJvmApplicationTemplate()
    )
}