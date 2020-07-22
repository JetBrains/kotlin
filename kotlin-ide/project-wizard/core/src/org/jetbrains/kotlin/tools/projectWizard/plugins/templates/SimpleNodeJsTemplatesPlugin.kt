/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.plugins.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.PluginSettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.templates.KtorServerTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.SimpleNodeJsTemplate

class SimpleNodeJsTemplatesPlugin(context: Context) : TemplatePlugin(context) {
    override val path = pluginPath

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks +
            listOf(
                KtorTemplatesPlugin.addTemplate,
            )

    val addTemplate by addTemplateTask(SimpleNodeJsTemplate())

    companion object : PluginSettingsOwner() {
        override val pluginPath = "template.simpleNodeJs"

        val addTemplate by addTemplateTask(KtorServerTemplate())
    }
}