package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.templates.*

class KotlinTestTemplatePlugin(context: Context) : TemplatePlugin(context) {
    val addTemplate by addTemplateTask(KotlinTestTemplate())
}