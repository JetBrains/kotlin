package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.PluginSettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask

class JpsPlugin(context: Context) : BuildSystemPlugin(context) {
    override val path = pluginPath

    companion object : PluginSettingsOwner() {
        override val pluginPath = "buildSystem.jps"
        val addBuildSystemData by addBuildSystemData(
            BuildSystemData(
                type = BuildSystemType.Jps,
                buildFileData = null
            )
        )
    }

    override val pipelineTasks: List<PipelineTask> = super.pipelineTasks + listOf(addBuildSystemData)
}