/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.plugins

import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.PluginSettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.UNIT_SUCCESS
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.Property
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.service.RunConfigurationsService
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin

class RunConfigurationsPlugin(context: Context) : Plugin(context) {
    override val path = pluginPath

    override val settings: List<PluginSetting<*, *>> = emptyList()
    override val pipelineTasks: List<PipelineTask> = listOf(createRunConfigurationsTask)
    override val properties: List<Property<*>> = listOf(configurations)

    companion object: PluginSettingsOwner() {
        override val pluginPath = "runConfigurations"

        val configurations by listProperty<WizardRunConfiguration>()

        val createRunConfigurationsTask by pipelineTask( GenerationPhase.PROJECT_IMPORT) {
            runBefore(BuildSystemPlugin.importProject)

            withAction {
                service<RunConfigurationsService>().apply {
                    addRunConfigurations(configurations.propertyValue)
                }
                UNIT_SUCCESS
            }
        }
    }
}