package org.jetbrains.kotlin.tools.projectWizard.plugins

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.JpsPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.MavenPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GroovyDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.KotlinDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.ConsoleJvmApplicationTemplatePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.KotlinTestTemplatePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin

object Plugins {
    val allPlugins = { context: Context ->
        listOf(
            StructurePlugin(context),

            GroovyDslPlugin(context),
            KotlinDslPlugin(context),
            JpsPlugin(context),
            MavenPlugin(context),

            KotlinPlugin(context),
            TemplatesPlugin(context),
            ProjectTemplatesPlugin(context),

            // templates
            KotlinTestTemplatePlugin(context),
            ConsoleJvmApplicationTemplatePlugin(context)
        )
    }
}