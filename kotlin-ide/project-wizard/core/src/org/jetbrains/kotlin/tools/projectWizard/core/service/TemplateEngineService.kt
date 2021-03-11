/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.RuntimeServices
import org.apache.velocity.runtime.log.LogChute
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTextDescriptor
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.io.StringWriter

abstract class TemplateEngineService : WizardService {
    abstract fun renderTemplate(template: FileTemplateDescriptor, data: Map<String, Any?>): String

    protected fun getTemplateText(template: FileTemplateDescriptor) = try {
        val templateId = template.templateId.replace('\\', '/')
        Template::class.java.getResource(templateId).readText()
    } catch (e: Throwable) {
        throw RuntimeException("Can not get template ${template.templateId}", e)
    }

    fun Writer.writeTemplate(template: FileTemplate): TaskResult<Unit> {
        val formatter = service<FileFormattingService>()
        val unformattedText = when (val descriptor = template.descriptor) {
            is FileTemplateDescriptor -> renderTemplate(descriptor, template.data)
            is FileTextDescriptor -> descriptor.text
        }
        val fileName = template.descriptor.relativePath?.fileName?.toString()
        val text = fileName?.let { formatter.formatFile(unformattedText, it) } ?: unformattedText
        return service<FileSystemWizardService>().createFile(template.rootPath / template.descriptor.relativePath, text)
    }
}


class VelocityTemplateEngineServiceImpl : TemplateEngineService(), IdeaIndependentWizardService {
    override fun renderTemplate(template: FileTemplateDescriptor, data: Map<String, Any?>): String {
        val templateText = getTemplateText(template)
        val context = VelocityContext().apply {
            data.forEach { (key, value) -> put(key, value) }
        }
        return StringWriter().use { writer ->
            runVelocityActionWithoutLogging { Velocity.evaluate(context, writer, "", templateText) }
            writer.toString()
        }
    }


    private fun runVelocityActionWithoutLogging(action: () -> Unit) {
        val initialLogger = Velocity.getProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM)
        Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, DoNothingVelocityLogger)
        action()
        if (initialLogger != null) {
            Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, initialLogger)
        }
    }

    private object DoNothingVelocityLogger : LogChute {
        override fun isLevelEnabled(level: Int): Boolean = false
        override fun init(rs: RuntimeServices?) = Unit
        override fun log(level: Int, message: String?) = Unit
        override fun log(level: Int, message: String?, t: Throwable?) = Unit
    }
}